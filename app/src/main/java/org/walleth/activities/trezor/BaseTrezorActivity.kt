package org.walleth.activities.trezor

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.Message
import com.satoshilabs.trezor.lib.TrezorException
import com.satoshilabs.trezor.lib.TrezorManager
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import kotlinx.android.synthetic.main.activity_trezor.*
import kotlinx.android.synthetic.main.password_input.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.bip44.BIP44
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.ligi.compat.HtmlCompat
import org.ligi.kaxt.inflate
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.BaseSubActivity
import org.walleth.activities.trezor.BaseTrezorActivity.STATES.*
import org.walleth.data.AppDatabase
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.ui.KEY_MAP_NUM_PAD
import org.walleth.ui.showPINDialog


abstract class BaseTrezorActivity : BaseSubActivity() {

    abstract fun handleExtraMessage(res: Message?)
    abstract fun handleAddress(address: Address)
    abstract fun getTaskSpecificMessage(): GeneratedMessageV3?

    protected var currentBIP44: BIP44? = null
    protected val appDatabase: AppDatabase by inject()
    protected val networkDefinitionProvider: NetworkDefinitionProvider by inject()

    protected val handler = Handler()
    private val manager by lazy { TrezorManager(this) }

    private var currentSecret = ""

    protected enum class STATES {
        REQUEST_PERMISSION,
        INIT,
        PIN_REQUEST,
        PWD_REQUEST,
        PWD_STATE,
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
        handler.post(mainRunnable)
    }

    protected val mainRunnable: Runnable = object : Runnable {
        override fun run() {
            if (manager.tryConnectDevice()) {

                trezor_connect_indicator.setImageResource(R.drawable.trezor_icon)
                trezor_status_text.visibility = View.GONE

                try {
                    GlobalScope.launch(Dispatchers.Main) {
                        val trezorResult = withContext(Dispatchers.Default) { manager.sendMessage(getMessageForState()) }
                        trezorResult.handleTrezorResult()
                    }

                } catch (trezorException: TrezorException) {
                    // this can happen when the trezor is unplugged - don't really care in this case
                    trezorException.printStackTrace()
                }

            } else {
                if (state == INIT && manager.hasDeviceWithoutPermission(true)) {
                    manager.requestDevicePermissionIfCan(true)
                    state = REQUEST_PERMISSION
                }

                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun Message.handleTrezorResult() {
        if (state == CANCEL) {
            cancel()
            return
        }


        when (this) {
            is TrezorMessage.PinMatrixRequest -> showPINDialog(
                    onCancel = {
                        state = CANCEL
                        handler.post(mainRunnable)
                    },
                    onPIN = { pin ->
                        currentSecret = pin
                        state = PIN_REQUEST
                        handler.post(mainRunnable)
                    },
                    pinPadMapping = KEY_MAP_NUM_PAD
            )
            is TrezorMessage.PassphraseRequest -> if (hasOnDevice() && onDevice) {
                enterNewState(PWD_ON_DEVICE)
            } else {
                showPassPhraseDialog()
            }
            is TrezorMessage.PassphraseStateRequest -> enterNewState(PWD_STATE)
            is TrezorMessage.ButtonRequest -> enterNewState(BUTTON_ACK)
            is TrezorMessage.Features -> {
                if (model != "1" && model != "T") {
                    alert("Only TREZOR model T and ONE supported - but found model: $model")
                }
                if (model == "T" && !(majorVersion == 2 && minorVersion == 1)) {
                    alert("For Trezor model T only Firmware 2.1.X is supported - please update your TREZOR") {
                        finish()
                    }
                } else if (model == "1" && !(majorVersion == 1 && minorVersion == 8)) {
                    alert("For Trezor model ONE only Firmware 2.1.X is supported - please update your TREZOR") {
                        finish()
                    }
                } else {
                    enterNewState(READ_ADDRESS)
                }
            }
            is TrezorMessage.EthereumAddress -> handleAddress(Address(address))
            is TrezorMessage.Failure -> when (code) {
                TrezorMessage.Failure.FailureType.Failure_PinInvalid -> alert(R.string.trezor_pin_invalid, R.string.dialog_title_error) { cancel() }
                TrezorMessage.Failure.FailureType.Failure_UnexpectedMessage -> Unit
                TrezorMessage.Failure.FailureType.Failure_ActionCancelled -> cancel()
                TrezorMessage.Failure.FailureType.Failure_ProcessError -> if (message.contains("not initialized")) {
                    alert(R.string.trezor_not_initialized, R.string.dialog_title_error) { cancel() }
                } else {
                    alert(getString(R.string.process_error, message))
                }
                else -> alert("problem: $message $code")
            }

            else -> handleExtraMessage(this)
        }
    }

    private fun cancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    protected open fun getMessageForState(): GeneratedMessageV3 = when (state) {
        INIT, REQUEST_PERMISSION -> TrezorMessage.Initialize.getDefaultInstance()
        READ_ADDRESS -> TrezorMessage.EthereumGetAddress.newBuilder()
                .addAllAddressN(currentBIP44!!.path.map { it.numberWithHardeningFlag })
                .build()
        PWD_STATE -> TrezorMessage.PassphraseStateAck.newBuilder().build()
        PWD_ON_DEVICE -> TrezorMessage.PassphraseAck.newBuilder().build()
        PIN_REQUEST -> TrezorMessage.PinMatrixAck.newBuilder().setPin(currentSecret).build()
        PWD_REQUEST -> TrezorMessage.PassphraseAck.newBuilder().setPassphrase(currentSecret).build()
        BUTTON_ACK -> TrezorMessage.ButtonAck.getDefaultInstance()
        CANCEL -> TrezorMessage.Cancel.newBuilder().build()
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
                    handler.post(mainRunnable)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    state = CANCEL
                    handler.post(mainRunnable)
                }

                .show()
    }

}
