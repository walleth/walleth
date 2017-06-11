package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Button
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.satoshilabs.trezor.lib.TrezorManager
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import kotlinx.android.synthetic.main.pinput.view.*
import org.kethereum.functions.toHexString
import org.ligi.kaxt.setVisibility
import org.walleth.R
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.keystore.WallethKeyStore

private val ADDRESS_HEX_KEY = "address_hex"
fun Intent.hasAddressResult() = hasExtra(ADDRESS_HEX_KEY)
fun Intent.getAddressResult() = getStringExtra(ADDRESS_HEX_KEY)

class TrezorCommunicatorActivity : AppCompatActivity() {

    val addressBook: AddressBook by LazyKodein(appKodein).instance()
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()

    val manager by lazy { TrezorManager(this) }
    val handler = Handler()
    var currentPin = ""

    enum class STATES {
        INIT,
        PIN_REQUEST,
        READ,
        CANCEL
    }

    var state: STATES = STATES.INIT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_trezor)

        supportActionBar?.subtitle = "TREZOR Hardware Wallet"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        handler.post(mainRunnable)
    }

    val mainRunnable: Runnable = object : Runnable {
        override fun run() {
            if (manager.tryConnectDevice()) {

                val res = when (state) {
                    STATES.INIT -> manager.sendMessage(TrezorMessage.Initialize.getDefaultInstance())
                    STATES.READ -> manager.sendMessage(TrezorMessage.EthereumGetAddress.getDefaultInstance())
                    STATES.PIN_REQUEST -> manager.sendMessage(TrezorMessage.PinMatrixAck.newBuilder().setPin(currentPin).build())
                    STATES.CANCEL -> manager.sendMessage(TrezorMessage.Cancel.newBuilder().build())

                    else -> null
                }

                if (state==STATES.CANCEL) {
                    finish()
                    return
                }

                when (res) {
                    is TrezorMessage.PinMatrixRequest -> {
                        showPINDialog()
                    }

                    is TrezorMessage.Failure -> {
                        if (res.code == TrezorType.FailureType.Failure_PinInvalid) {
                            Snackbar.make(window.decorView, "Pin invalid", Snackbar.LENGTH_LONG).show()
                            handler.post(this)
                        } else {

                        }
                    }
                }
                if (res is TrezorMessage.EthereumAddress) {
                    val resultIntent = Intent()
                    resultIntent.putExtra(ADDRESS_HEX_KEY, res.address.toByteArray().toHexString())
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                if (res is TrezorMessage.Features) {
                    if (res.pinProtection) {
                        state = STATES.READ
                        handler.post(this)
                    } else {
                        state = STATES.READ
                        handler.post(this)
                    }
                }
            } else {
                if (state == STATES.INIT && manager.hasDeviceWithoutPermission(true)) {
                    manager.requestDevicePermissionIfCan(true)
                }

                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun showPINDialog() {
        val inflater = LayoutInflater.from(this@TrezorCommunicatorActivity)
        val view = inflater.inflate(R.layout.pinput, null)
        var dialogPin = ""
        val displayPin = {
            view.pin_textview.text = (0..(dialogPin.length - 1)).map { "*" }.joinToString("")
            view.pin_back.setVisibility(!dialogPin.isEmpty())
        }
        displayPin.invoke()
        val pinPadMapping = arrayOf(7, 8, 9, 4, 5, 6, 1, 2, 3)
        for (i in 0..8) {
            val button = Button(this@TrezorCommunicatorActivity)
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
        AlertDialog.Builder(this@TrezorCommunicatorActivity)
                .setView(view)
                .setTitle("Please enter your PIN")
                .setPositiveButton(android.R.string.ok, { _, _ ->
                    currentPin = dialogPin
                    state = STATES.PIN_REQUEST
                    handler.post(mainRunnable)
                })
                .setNegativeButton(android.R.string.cancel, { _, _ ->
                    state = STATES.CANCEL
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
