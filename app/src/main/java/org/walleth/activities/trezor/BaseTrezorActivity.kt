package org.walleth.activities.trezor

import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.Message
import com.satoshilabs.trezor.lib.TrezorManager
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import kotlinx.android.synthetic.main.activity_trezor.*
import kotlinx.android.synthetic.main.password_input.view.*
import kotlinx.android.synthetic.main.pinput.view.*
import org.kethereum.bip44.BIP44
import org.kethereum.model.Address
import org.ligi.compat.HtmlCompat
import org.ligi.kaxt.setVisibility
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.trezor.BaseTrezorActivity.STATES.*
import org.walleth.data.AppDatabase
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.transactions.TransactionProvider
import org.walleth.khex.toHexString


abstract class BaseTrezorActivity : AppCompatActivity() {

    var currentBIP44: BIP44? = null
    abstract fun handleExtraMessage(res: Message?)
    abstract fun handleAddress(address: Address)
    abstract fun getTaskSpecificMessage(): GeneratedMessageV3?

    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    val appDatabase: AppDatabase by LazyKodein(appKodein).instance()
    val transactionProvider: TransactionProvider by LazyKodein(appKodein).instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()

    val inflater by lazy { LayoutInflater.from(this)!! }

    val manager by lazy { TrezorManager(this) }
    val handler = Handler()
    var currentSecret = ""

    enum class STATES {
        REQUEST_PERMISSION,
        INIT,
        PIN_REQUEST,
        PWD_REQUEST,
        BUTTON_ACK,
        PROCESS_TASK,
        READ_ADDRESS,
        CANCEL
    }

    var state: STATES = INIT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_trezor)

        trezor_status_text.text = HtmlCompat.fromHtml(getString(R.string.connect_trezor_message))
        trezor_status_text.movementMethod = LinkMovementMethod()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    protected fun enterNewState(newState: STATES) {
        state = newState
        handler.post(mainRunnable)
    }

    val mainRunnable: Runnable = object : Runnable {
        override fun run() {
            if (manager.tryConnectDevice()) {

                trezor_connect_indicator.setImageResource(R.drawable.trezor_icon)
                trezor_status_text.visibility = View.GONE

                val res = manager.sendMessage(getMessageForState())

                if (state == CANCEL) {
                    finish()
                    return
                }


                when (res) {
                    is TrezorMessage.PinMatrixRequest -> showPINDialog()
                    is TrezorMessage.PassphraseRequest -> showPassPhraseDialog()
                    is TrezorMessage.ButtonRequest -> enterNewState(BUTTON_ACK)
                    is TrezorMessage.Features -> enterNewState(READ_ADDRESS)
                    is TrezorMessage.EthereumAddress -> handleAddress(Address(res.address.toByteArray().toHexString()))

                    is TrezorMessage.Failure -> {
                        if (res.code == TrezorType.FailureType.Failure_PinInvalid) {
                            alert("Pin invalid", "Error", OnClickListener { _, _ ->
                                finish()
                            })
                        } else if (res.code == TrezorType.FailureType.Failure_ActionCancelled) {
                            finish()
                        } else {
                            alert(res.message)
                        }
                    }

                    else -> handleExtraMessage(res)
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

    protected open fun getMessageForState(): GeneratedMessageV3 {
        return when (state) {
            INIT, REQUEST_PERMISSION -> TrezorMessage.Initialize.getDefaultInstance()
            READ_ADDRESS -> TrezorMessage.EthereumGetAddress.newBuilder()
                    .addAllAddressN(currentBIP44!!.toIntList())
                    .build()
            PIN_REQUEST -> TrezorMessage.PinMatrixAck.newBuilder().setPin(currentSecret).build()
            PWD_REQUEST -> TrezorMessage.PassphraseAck.newBuilder().setPassphrase(currentSecret).build()
            BUTTON_ACK -> TrezorMessage.ButtonAck.getDefaultInstance()
            CANCEL -> TrezorMessage.Cancel.newBuilder().build()
            PROCESS_TASK -> getTaskSpecificMessage()!!
        }
    }

    protected fun showPassPhraseDialog() {
        val input_layout = layoutInflater.inflate(R.layout.password_input, null)
        AlertDialog.Builder(this)
                .setView(input_layout)
                .setTitle("Please enter your passphrase")
                .setPositiveButton(android.R.string.ok, { _, _ ->
                    currentSecret = input_layout.password_input.text.toString()
                    state = PWD_REQUEST
                    handler.post(mainRunnable)
                })
                .setNegativeButton(android.R.string.cancel, { _, _ ->
                    state = CANCEL
                    handler.post(mainRunnable)
                })

                .show()
    }


    private fun showPINDialog() {
        val view = inflater.inflate(R.layout.pinput, null)
        var dialogPin = ""
        val displayPin = {
            view.pin_textview.text = (0..(dialogPin.length - 1)).map { "*" }.joinToString("")
            view.pin_back.setVisibility(!dialogPin.isEmpty())
        }
        displayPin.invoke()
        val pinPadMapping = arrayOf(7, 8, 9, 4, 5, 6, 1, 2, 3)
        for (i in 0..8) {
            val button = Button(this)
            button.text = "*"
            button.setOnClickListener {
                if (dialogPin.length <= 10)
                    dialogPin += pinPadMapping[i]
                displayPin.invoke()
            }
            view.pin_grid.addView(button)
        }
        view.pin_back.setOnClickListener {
            if (dialogPin.isNotEmpty())
                dialogPin = dialogPin.substring(0, dialogPin.length - 1)
            displayPin.invoke()
        }
        AlertDialog.Builder(this)
                .setView(view)
                .setTitle("Please enter your PIN")
                .setPositiveButton(android.R.string.ok, { _, _ ->
                    currentSecret = dialogPin
                    state = PIN_REQUEST
                    handler.post(mainRunnable)
                })
                .setNegativeButton(android.R.string.cancel, { _, _ ->
                    state = CANCEL
                    handler.post(mainRunnable)
                })
                .show()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
