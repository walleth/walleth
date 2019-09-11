package org.walleth.activities.nfc

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.functions.encodeRLP
import org.kethereum.keccakshortcut.keccak
import org.koin.android.ext.android.inject
import org.walleth.activities.trezor.TREZOR_REQUEST_CODE
import org.walleth.data.AppDatabase
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.kethereum.android.TransactionParcel
import org.walleth.khartwarewallet.KHardwareChannel
import org.walleth.khex.toHexString


fun Activity.startNFCSigningActivity(transactionParcel: TransactionParcel) {
    val nfcIntent = Intent(this, NFCSignTransactionActivity::class.java).putExtra("TX", transactionParcel)
    startActivityForResult(nfcIntent, TREZOR_REQUEST_CODE)
}

class NFCSignTransactionActivity : NFCBaseActivityWithPINHandling() {

    private val transaction by lazy { intent.getParcelableExtra<TransactionParcel>("TX") }

    private val appDatabase: AppDatabase by inject()

    override fun afterCorrectPin(channel: KHardwareChannel) {
        val oldHash = transaction.transaction.txHash

        val signedTransaction = channel.sign(transaction.transaction)

        setText("signed")
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                transaction.transaction.txHash = signedTransaction.encodeRLP().keccak().toHexString()
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
