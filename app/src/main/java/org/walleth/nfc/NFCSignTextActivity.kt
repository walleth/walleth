package org.walleth.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kethereum.crypto.toAddress
import org.kethereum.crypto.toHex
import org.kethereum.model.Address
import org.kethereum.model.SignatureData
import org.walleth.khartwarewallet.KHardwareChannel
import org.walleth.khex.hexToByteArray


private const val KEY_TEXT = "TEXT"
private const val KEY_ADDRESS = "ADDRESS"

fun Activity.getNFCSignTextIntent(text: String, address: String): Intent = Intent(this, NFCSignTextActivity::class.java)
        .putExtra(KEY_TEXT, text)
        .putExtra(KEY_ADDRESS, address)


private fun Byte.toByteArray() = ByteArray(1) { this }

fun signWithEIP191(version: Byte, versionSpecificData: ByteArray, message: ByteArray, signMessage: (ba: ByteArray) -> SignatureData) =
        signMessage(0x19.toByte().toByteArray() + version.toByteArray() + versionSpecificData + message)

fun signWithEIP191PersonalSign(message: ByteArray, signMessage: (ba: ByteArray) -> SignatureData) =
        signWithEIP191(0x45, ("thereum Signed Message:\n" + message.size).toByteArray(), message, signMessage)

class NFCSignTextActivity : NFCBaseActivityWithPINHandling() {

    private val textToSign by lazy { intent.getStringExtra(KEY_TEXT) }
    private val addressToSignFor by lazy { intent.getStringExtra(KEY_ADDRESS) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = "Sign Text via NFC"
    }

    override fun afterCorrectPin(channel: KHardwareChannel) {

        val address = channel.toPublicKey().toAddress()

        if (Address(addressToSignFor) != address) {
            setText("The given card does not match the account")
        } else {
            setText("signing")


            val signed = signWithEIP191PersonalSign(textToSign.hexToByteArray()) {
                channel.signByteArray(it)
            }

            setText("signed")
            lifecycleScope.launch(Dispatchers.Main) {
                setResult(RESULT_OK, Intent().apply { putExtra("HEX", signed.toHex()) })
                finish()
            }
        }
    }

}
