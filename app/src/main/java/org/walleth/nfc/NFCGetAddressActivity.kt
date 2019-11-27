package org.walleth.nfc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.os.Bundle
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_nfc.*
import org.kethereum.crypto.toAddress
import org.kethereum.model.Address
import org.ligi.kaxtui.alert
import org.walleth.data.*
import org.walleth.data.addresses.AccountKeySpec
import org.walleth.khartwarewallet.KHardwareChannel

@Parcelize
data class NFCCredentials(var isNewCard: Boolean,
                          var pin: String,
                          var pairingPassword: String,
                          var puk: String? = null) : Parcelable

class NFCGetAddressActivity : BaseNFCActivity() {

    private var credentials: NFCCredentials? = null

    fun setText(value: String) {
        runOnUiThread {
            nfc_status_text.text = value

            nfc_status_text.parent.requestLayout()
        }
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (credentials == null) {
            startActivityForResult(Intent(this, NFCEnterCredentialsActivity::class.java), REQUEST_CODE_ENTER_NFC_CREDENTIALS)
        }

        supportActionBar?.subtitle = "NFC SmartCard interaction"

        cardManager.onCardConnectedListener = { channel ->

            credentials?.let { credentials ->

                try {

                    val cardInfo = channel.getCardInfo()
                    if (credentials.isNewCard) {
                        if (!cardInfo.isInitializedCard) {
                            setText("Initializing new card ..")
                            val result = channel.commandSet.init(credentials.pin, credentials.puk!!, credentials.pairingPassword)
                            if (!result.checkOK().isOK) {
                                setText("Initializing failed")
                            } else {

                                channel.commandSet.select().checkOK()
                                setText("Selected")

                                pairAndStore(channel, credentials)

                                channel.commandSet.autoOpenSecureChannel()

                                setText("Secured channel")
                                channel.commandSet.verifyPIN(credentials.pin)

                                channel.commandSet.setNDEF((NdefMessage(NdefRecord.createApplicationRecord("org.walleth")).toByteArray()))

                                setText("NDEF set")
                                val wasKeyGenerated = channel.commandSet.generateKey().isOK


                                if (wasKeyGenerated) {
                                    finishWithAddress(channel.toPublicKey().toAddress())
                                } else {
                                    setText("Problem generating key")
                                }

                            }
                        } else runOnUiThread {
                            alert("This is not a new card.")
                        }
                    } else {

                        if (nfcCredentialStore.hasPairing(cardInfo.instanceUID)) {
                            channel.commandSet.pairing = nfcCredentialStore.getPairing(cardInfo.instanceUID)
                            setText("Paired (old)")
                        } else {
                            pairAndStore(channel, credentials)
                            setText("Paired (new)")
                        }

                        channel.commandSet.autoOpenSecureChannel()

                        setText("Secured channel")
                        channel.commandSet.verifyPIN(credentials.pin)

                        setText("PIN")

                        if (!cardInfo.hasMasterKey()) {
                            val wasKeyGenerated = channel.commandSet.generateKey().isOK
                            if (!wasKeyGenerated) {
                                setText("Could not generate key")
                                return@let
                            }
                        }

                        val address = channel.toPublicKey().toAddress()

                        setText("got address$address")

                        finishWithAddress(address)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        alert(e.message!!)
                    }
                }

            }
        }

        cardManager.start()
    }

    private fun pairAndStore(channel: KHardwareChannel, credentials: NFCCredentials) {
        channel.commandSet.autoPair(credentials.pairingPassword)
        setText("Paired")
        val keyUID = channel.getCardInfo().instanceUID
        nfcCredentialStore.putPairing(keyUID, channel.commandSet.pairing)
    }

    private fun finishWithAddress(address: Address) {
        val resultIntent = Intent().putExtra(EXTRA_KEY_ADDRESS, address.toString()).putExtra(EXTRA_KEY_ACCOUNTSPEC, AccountKeySpec(ACCOUNT_TYPE_NFC))
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        cardManager.onCardConnectedListener = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            credentials = data.getParcelableExtra(EXTRA_KEY_NFC_CREDENTIALS)
        } else {
            finish()
        }
    }
}
