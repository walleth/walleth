package org.walleth.activities.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_nfc.*
import org.kethereum.crypto.toAddress
import org.ligi.kaxtui.alert

private const val ADDRESS_HEX_KEY = "address_hex"
private const val ADDRESS_PATH = "address_path"
fun Intent.hasAddressResult() = hasExtra(ADDRESS_HEX_KEY)
fun Intent.getAddressResult() = getStringExtra(ADDRESS_HEX_KEY)
fun Intent.getPATHResult() = getStringExtra(ADDRESS_PATH)

class NFCGetAddressActivity : BaseNFCActivity() {

    var pin = "000000"

    fun setText(value:String) {
        runOnUiThread {
            nfc_status_text.text = value

            nfc_status_text.parent.requestLayout()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = "NFC SmartCard interaction"
        /*
        showPINDialog(
                onPIN = { pin = it },
                onCancel = { finish() },
                labelButtons = true,
                pinPadMapping = KEY_MAP_TOUCH,
                maxLength = 6
        )
        */

        cardManager.onCardConnectedListener = { channel ->

            try {

                setText("Connected")

                channel.autoPair("WalletAppletTest")

                setText("Paired")
                channel.autoOpenSecureChannel()

                setText("Secured channel")
                channel.verifyPIN(pin)

                setText("PIN")
                val address = channel.toPublicKey().toAddress()

                setText("Address")

                channel.unpairOthers()
                channel.autoUnpair()

                setText("Unpaired")
                val resultIntent = Intent()
                resultIntent.putExtra(ADDRESS_HEX_KEY, address.hex)
                //resultIntent.putExtra(ADDRESS_PATH, BIP44(DEFAULT_ETHEREUM_BIP44_PATH))
                setResult(Activity.RESULT_OK, resultIntent)
                finish()

            } catch (e: Exception) {
                runOnUiThread {
                    alert(e.message!!)
                }
            }


        }

        cardManager.start()
    }

    override fun onDestroy() {
        super.onDestroy()

        cardManager.onCardConnectedListener = null
    }
}
