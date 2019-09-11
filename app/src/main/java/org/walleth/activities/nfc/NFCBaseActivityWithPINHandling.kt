package org.walleth.activities.nfc

import android.os.Bundle
import im.status.keycard.io.WrongPINException
import kotlinx.android.synthetic.main.activity_nfc.*
import org.kethereum.crypto.toAddress
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.activities.showAccountPinDialog
import org.walleth.data.AppDatabase
import org.walleth.kethereum.android.TransactionParcel
import org.walleth.khartwarewallet.KHardwareChannel

abstract class NFCBaseActivityWithPINHandling : BaseNFCActivity() {

    private val transaction by lazy { intent.getParcelableExtra<TransactionParcel>("TX") }

    private val appDatabase: AppDatabase by inject()

    lateinit var pin: String

    fun setText(value: String) {
        runOnUiThread {
            nfc_status_text.text = value

            nfc_status_text.parent.requestLayout()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showAccountPinDialog {
            if (it != null) {
                pin = it

                cardManager.onCardConnectedListener = { channel ->

                    try {

                        if (!nfcCredentialStore.hasPairing(channel.getCardInfo().instanceUID)) {
                            setText("no pairing")
                        } else {
                            setText("pairing")

                            channel.commandSet.pairing = nfcCredentialStore.getPairing(channel.getCardInfo().instanceUID)

                            setText("open secure channel")
                            channel.commandSet.autoOpenSecureChannel()

                            if (channel.getStatus().pinRetryCount == 0) {
                                setText("too many PIN tries with this card. This card needs to be unblocked first.")
                            } else {
                                setText("verify PIN")
                                channel.commandSet.verifyPIN(pin).checkAuthOK()

                                val address = channel.toPublicKey().toAddress()

                                if (transaction.transaction.from != address) {
                                    setText("The given card does not match the account")
                                } else {
                                    setText("signing")

                                    afterCorrectPin(channel)
                                }
                            }
                        }
                    } catch (wrongPin: WrongPINException) {
                        runOnUiThread {
                            alert("Invalid PIN.\nYou have " + channel.getStatus().pinRetryCount + " retries left.") {
                                finish()
                            }
                        }
                    } catch (e: Exception) {
                        setText("error " + e.message)
                        e.printStackTrace()
                        e.message?.let {
                            runOnUiThread {
                                alert(it)
                            }
                        }
                    }
                }


            }

            cardManager.start()
        }

        supportActionBar?.subtitle = "Sign via NFC"
    }

    internal abstract fun afterCorrectPin(channel: KHardwareChannel)

    override fun onDestroy() {
        super.onDestroy()

        cardManager.onCardConnectedListener = null
    }
}