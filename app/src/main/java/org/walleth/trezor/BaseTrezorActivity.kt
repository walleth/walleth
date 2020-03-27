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
import org.ligi.tracedroid.logging.Log
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.chains.ChainInfoProvider
import org.walleth.credentials.KEY_MAP_NUM_PAD
import org.walleth.credentials.showPINDialog
import org.walleth.data.AppDatabase
import org.walleth.khartwarewallet.trezor.tryConnectTrezor
import org.walleth.trezor.BaseTrezorActivity.STATES.*

abstract class BaseTrezorActivity : BaseSubActivity() {

    abstract fun handleExtraMessage(res: Message<*, *>?)
    abstract fun handleAddress(address: Address)
    abstract fun getTaskSpecificMessage(): Message<*, *>?

    protected var currentBIP44: BIP44? = null
    protected val appDatabase: AppDatabase by inject()
    protected val chainInfoProvider: ChainInfoProvider by inject()
    private var currentSecret = ""
    protected var isKeepKey = false

    protected enum class STATES {
        REQUEST_PERMISSION,
        INIT,
        PIN_REQUEST,
        PWD_REQUEST,
        PWD_ON_DEVICE,
        BUTTON_ACK,
        PROCESS_TASK,
        READ_ADDRESS,
        CANCEL
    }

    private var state: STATES = INIT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_trezor)

        trezor_status_text.text = HtmlCompat.fromHtml(getString(R.string.connect_trezor_message))
        trezor_status_text.movementMethod = LinkMovementMethod()
    }

    protected fun enterNewState(newState: STATES) {
        state = newState
        connectAndExecute()
    }

    fun connectAndExecute() = lifecycleScope.launch(Dispatchers.IO) {
        tryConnectTrezor(this@BaseTrezorActivity,
                onPermissionDenied = {},
                onDeviceConnected = {
                    val m = getMessageForState()
                    val result = it.exchangeMessage(m)
                    result?.handleTrezorResult()
                    it.disconnect()
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
                    enterNewState(PWD_ON_DEVICE)
                } else {
                    showPassPhraseDialog()
                }
                is ButtonRequest -> enterNewState(BUTTON_ACK)
                is Features -> {
                    if (model != "1" && model != "T" && !model.startsWith("K1")) {
                        alert("Only TREZOR model T and ONE supported - but found model: $model")
                    }
                    if (model == "T" && !(major_version == 2 && minor_version == 1)) {
                        alert("For Trezor model T only Firmware 2.1.X is supported but found $major_version.$minor_version.$patch_version") {
                            finish()
                        }
                    } else if (model == "1" && !(major_version == 1 && minor_version == 8)) {
                        alert("For Trezor model ONE only Firmware 1.8.X is supported but found $major_version.$minor_version.$patch_version") {
                            finish()
                        }
                    } else {
                        isKeepKey = model.startsWith("K1")
                        enterNewState(READ_ADDRESS)
                    }
                }
                is EthereumAddress -> {
                    handleAddress(Address(if (address != null) {
                        address.toString().removePrefix("[hex=").removeSuffix("]")
                    } else {
                        address_str
                    }))
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

    private fun cancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    protected open fun getMessageForState(): Message<*, *> = when (state) {
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
