package org.walleth.activities.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_nfc.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.kethereum.crypto.toAddress
import org.kethereum.functions.encodeRLP
import org.kethereum.keccakshortcut.keccak
import org.kodein.di.generic.instance
import org.ligi.kaxtui.alert
import org.ligi.kroom.inTransaction
import org.walleth.activities.trezor.TREZOR_REQUEST_CODE
import org.walleth.data.AppDatabase
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.kethereum.android.TransactionParcel
import org.walleth.khex.toHexString

const val TREZOR_REQUEST_CODE = 7689

private const val ADDRESS_HEX_KEY = "address_hex"
private const val ADDRESS_PATH = "address_path"

fun Activity.startNFCSigningActivity(transactionParcel: TransactionParcel) {
    val trezorIntent = Intent(this, NFCSignTransactionActivity::class.java).putExtra("TX", transactionParcel)
    startActivityForResult(trezorIntent, TREZOR_REQUEST_CODE)
}

class NFCSignTransactionActivity : BaseNFCActivity() {

    private val transaction by lazy { intent.getParcelableExtra<TransactionParcel>("TX") }

    protected val appDatabase: AppDatabase by instance()

    var pin = "000000"

    fun setText(value: String) {
        runOnUiThread {
            nfc_status_text.text = value

            nfc_status_text.parent.requestLayout()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = "Sign via NFC"

        cardManager.onCardConnectedListener = { channel ->

            try {

                setText("Connected")

                channel.autoPair("WalletAppletTest")

                setText("Paired")
                channel.autoOpenSecureChannel()

                setText("Secured channel")
                channel.verifyPIN(pin)

                setText("PIN")

                val oldHash = transaction.transaction.txHash

                val address = channel.toPublicKey().toAddress()

                val signedTransaction = channel.sign(transaction.transaction)

                setText("Signing as $address")

                channel.unpairOthers()
                channel.autoUnpair()

                setText("Unpaired")



                GlobalScope.async (Dispatchers.Main) {
                    withContext(Dispatchers.Default) {
                        transaction.transaction.txHash = signedTransaction.encodeRLP().keccak().toHexString()
                        appDatabase.inTransaction {
                            oldHash?.let { transactions.deleteByHash(it) }
                            transactions.upsert(transaction.transaction.toEntity(signedTransaction.signatureData, TransactionState()))
                        }
                    }
                    setResult(RESULT_OK, Intent().apply { putExtra("TXHASH", transaction.transaction.txHash) })
                    finish()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.let {
                    runOnUiThread {
                        alert(it)
                    }
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
