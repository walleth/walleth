package org.walleth.trezor

import android.app.Activity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.squareup.wire.Message
import io.trezor.deviceprotocol.*
import kotlinx.android.synthetic.main.activity_trezor.*
import kotlinx.android.synthetic.main.password_input.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.komputing.kbip44.BIP44
import org.ligi.compat.HtmlCompat
import org.ligi.kaxt.inflate
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.chains.ChainInfoProvider
import org.walleth.credentials.KEY_MAP_NUM_PAD
import org.walleth.credentials.showPINDialog
import org.walleth.data.AppDatabase
import org.walleth.khartwarewallet.trezor.tryConnectTrezor
import org.walleth.trezor.BaseTrezorActivity.STATES.*

const val KEY_KEEPKEY_MODE = "KEEPKEY_MODE"

abstract class BaseTrezorActivity : BaseSubActivity() {

    private val isKeepKeyMode by lazy { intent.extras?.getBoolean(KEY_KEEPKEY_MODE) == true }
    abstract fun handleExtraMessage(res: Message<*, *>?)
    abstract fun handleAddress(address: Address)
    abstract suspend fun getTaskSpecificMessage(): Message<*, *>?

    protected var currentBIP44: BIP44? = null
    protected val appDatabase: AppDatabase by inject()
    protected val chainInfoProvider: ChainInfoProvider by inject()
    private var currentSecret = ""
    protected var isKeepKeyDevice = false
    protected var currentDeviceName: String? = null

    internal enum class STATES {
        REQUEST_PERMISSION,
        INIT,
        PIN_REQUEST,
        PWD_REQUEST,
        PWD_ON_DEVICE,
        BUTTON_ACK,
        PROCESS_TASK,
        READ_ADDRESS,
        IDLE,
        CANCEL
    }

    internal var state: STATES = INIT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_trezor)

        device_status_text.text = HtmlCompat.fromHtml(getString(getConnectMessage()))
        device_status_text.movementMethod = LinkMovementMethod()

        device_connect_indicator.setImageResource(if (isKeepKeyMode) R.drawable.keepkey else R.drawable.trezor_connect)
    }

    private fun getConnectMessage() = if (isKeepKeyMode) R.string.connect_keepkey_message else R.string.connect_trezor_message
    private fun getActionMessage() = if (isKeepKeyMode) R.string.interact_keepkey_message else R.string.interact_trezor_message

    internal open fun enterState(newState: STATES, withConnect: Boolean = true) {
        state = newState
        if (withConnect) {
            connectAndExecute()
        }
    }

    fun connectAndExecute() = lifecycleScope.launch(Dispatchers.IO) {
        tryConnectTrezor(this@BaseTrezorActivity,
            onPermissionDenied = {
                finishingAlert("Without you granting permission for this device WallETH is not able to talk to the device")
            },
            onDeviceConnected = {
                lifecycleScope.launch(Dispatchers.Main) {
                    getMessageForState()?.let { messageForState ->
                        val result = it.exchangeMessage(messageForState)
                        result?.handleTrezorResult()
                        it.disconnect()
                    }
                }
            })

    }


    private fun Message<*, *>.handleTrezorResult() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (state == CANCEL) {
                cancel()
                return@launch
            }


            when (this@handleTrezorResult) {
                is PinMatrixRequest -> showPINDialog(
                    onCancel = {
                        state = CANCEL
                        connectAndExecute()
                    },
                    onPIN = { pin ->
                        currentSecret = pin
                        state = PIN_REQUEST
                        connectAndExecute()
                    },
                    pinPadMapping = KEY_MAP_NUM_PAD
                )
                is PassphraseRequest -> if (_on_device == true) {
                    enterState(PWD_ON_DEVICE)
                } else {
                    showPassPhraseDialog()
                }
                is ButtonRequest -> enterState(BUTTON_ACK)
                is Features -> {
                    val version = KotlinVersion(major_version, minor_version, patch_version)
                    val potentialError = checkTrezorCompatibility(version, model)
                    if (potentialError != null) {
                        finishingAlert(potentialError)
                    } else {
                        isKeepKeyDevice = model.startsWith("K1")
                        currentDeviceName = label
                        when {
                            isKeepKeyDevice && isKeepKeyDevice != isKeepKeyMode -> finishingAlert("this is not a TREZOR - this is a KeepKey")
                            !isKeepKeyDevice && isKeepKeyDevice != isKeepKeyMode -> finishingAlert("this is not a KeepKey - this is a TREZOR")
                            else -> {
                                device_status_text.text = HtmlCompat.fromHtml(getString(getActionMessage()))
                                enterState(READ_ADDRESS)
                            }
                        }
                    }
                }
                is EthereumAddress -> {
                    enterState(IDLE, false)

                    handleAddress(
                        Address(
                            if (address != null) {
                                address.toString().removePrefix("[hex=").removeSuffix("]")
                            } else {
                                address_str
                            }
                        )
                    )

                }
                is Failure -> when (code) {
                    FailureType.Failure_PinInvalid -> alert(R.string.trezor_pin_invalid, R.string.dialog_title_error) { cancel() }
                    FailureType.Failure_UnexpectedMessage -> Unit
                    FailureType.Failure_ActionCancelled -> cancel()
                    FailureType.Failure_NotInitialized -> if (message.contains("not initialized")) {
                        alert(R.string.trezor_not_initialized, R.string.dialog_title_error) { cancel() }
                    } else {
                        alert(getString(R.string.process_error, message))
                    }
                    else -> alert("problem: $message $code")
                }

                else -> handleExtraMessage(this@handleTrezorResult)
            }
        }
    }

    private fun finishingAlert(message: String) = lifecycleScope.launch(Dispatchers.Main) {
        alert(message, "Error") {
            finish()
        }
    }

    private fun cancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    protected open suspend fun getMessageForState(): Message<*, *>? = when (state) {
        INIT, REQUEST_PERMISSION -> Initialize.Builder().build()
        READ_ADDRESS -> {
            EthereumGetAddress.Builder()
                .address_n(currentBIP44!!.path.map { it.numberWithHardeningFlag })
                .build()
        }
        PWD_ON_DEVICE -> PassphraseAck.Builder().build()
        PIN_REQUEST -> PinMatrixAck.Builder().pin(currentSecret).build()
        PWD_REQUEST -> PassphraseAck.Builder().passphrase(currentSecret).build()
        BUTTON_ACK -> ButtonAck.Builder().build()
        CANCEL -> Cancel.Builder().build()
        PROCESS_TASK -> getTaskSpecificMessage()!!
        IDLE -> null
    }


    private fun showPassPhraseDialog() {
        val inputLayout = inflate(R.layout.password_input)
        AlertDialog.Builder(this)
            .setView(inputLayout)
            .setTitle(R.string.trezor_please_enter_your_passphrase)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                currentSecret = inputLayout.password_input.text.toString()
                state = PWD_REQUEST
                connectAndExecute()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                state = CANCEL
                connectAndExecute()
            }
            .setCancelable(false)
            .show()
    }

}
