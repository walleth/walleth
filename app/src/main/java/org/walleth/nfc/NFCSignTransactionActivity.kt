package org.walleth.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.crypto.toAddress
import org.kethereum.extensions.transactions.encode
import org.kethereum.extensions.transactions.encodeLegacyTxRLP
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.SignedTransaction
import org.koin.android.ext.android.inject
import org.komputing.khex.extensions.toHexString
import org.walleth.data.AppDatabase
import org.walleth.data.REQUEST_CODE_NFC
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.kethereum.android.TransactionParcel
import org.walleth.khartwarewallet.KHardwareChannel
import java.lang.IllegalStateException

private const val KEY_TRANSACTION = "TX"

fun Activity.startNFCSigningActivity(transactionParcel: TransactionParcel) {

    val nfcIntent = Intent(this, NFCSignTransactionActivity::class.java).putExtra(KEY_TRANSACTION, transactionParcel)
    startActivityForResult(nfcIntent, REQUEST_CODE_NFC)
}

class NFCSignTransactionActivity : NFCBaseActivityWithPINHandling() {

    private val appDatabase: AppDatabase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = "Sign Transaction via NFC"
    }

    override fun afterCorrectPin(channel: KHardwareChannel) {

        val address = channel.toPublicKey().toAddress()
        val transaction = intent.getParcelableExtra<TransactionParcel>("TX")

        when {
            transaction == null -> {
                throw IllegalStateException("Got no transaction from intent in afterCorrectPin(channel)")
            }
            transaction.transaction.from != address -> {
                setText("The given card does not match the account")
            }
            else -> {
                setText("signing")


                val oldHash = transaction.transaction.txHash

                val signedTransaction: SignedTransaction = channel.signTransaction(transaction.transaction)

                setText("signed")
                lifecycleScope.launch(Dispatchers.Main) {
                    withContext(Dispatchers.Default) {
                        transaction.transaction.txHash = signedTransaction.encode().keccak().toHexString()
                        appDatabase.runInTransaction {
                            oldHash?.let { appDatabase.transactions.deleteByHash(it) }
                            appDatabase.transactions.upsert(transaction.transaction.toEntity(signedTransaction.signatureData, TransactionState()))
                        }
                    }
                    setResult(RESULT_OK, Intent().apply { putExtra("TXHASH", transaction.transaction.txHash) })
                    finish()
                }
            }
        }
    }

}
