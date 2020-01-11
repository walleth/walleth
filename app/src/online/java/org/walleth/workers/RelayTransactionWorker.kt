package org.walleth.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import org.kethereum.functions.encodeRLP
import org.kethereum.model.ChainId
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.komputing.khex.extensions.toHexString
import org.ligi.tracedroid.logging.Log
import org.walleth.data.AppDatabase
import org.walleth.data.KEY_TX_HASH
import org.walleth.data.rpc.RPCProvider
import org.walleth.data.transactions.TransactionEntity
import org.walleth.data.transactions.setHash

class RelayTransactionWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams), KoinComponent {


    private val okHttpClient: OkHttpClient by inject()
    private val appDatabase: AppDatabase by inject()
    private val rpcProvider: RPCProvider by inject()

    override suspend fun doWork(): Result {

        val txHash = inputData.getString(KEY_TX_HASH)
        val transaction: TransactionEntity? = txHash?.let { appDatabase.transactions.getByHash(it) }

        if (transaction == null) {
            Log.i("Cannot load address with $txHash")
            return Result.failure()
        }

        val chain = transaction.transaction.chain

        val rpc = chain?.let { rpcProvider.getForChain(ChainId(it)) }

        if (rpc == null) {
            transaction.setError("RPC not found for chain $chain")
            return Result.failure()
        }

        try {
            val result = rpc.sendRawTransaction(transaction.transaction.encodeRLP(transaction.signatureData).toHexString())

            return if (result != null) {
                transaction.setHash(if (!result.startsWith("0x")) "0x$result" else result)

                transaction.transactionState.eventLog = transaction.transactionState.eventLog ?: "" + "relayed"
                markSuccess(transaction)
            } else {
                transaction.setError("Could not (yet) relay transaction")
                Result.retry()
            }
        } catch (e: Exception) {
            return if (e.message == "Transaction with the same hash was already imported." || e.message?.startsWith("known transaction") == true) {
                markSuccess(transaction)
            } else {
                transaction.transactionState.eventLog = transaction.transactionState.eventLog ?: "" + "ERROR: ${e.message}\n"

                transaction.setError(e.message)
                appDatabase.transactions.upsert(transaction)

                Result.failure()
            }
        }
    }

    private fun markSuccess(transaction: TransactionEntity): Result {
        transaction.transactionState.relayed = "via RPC"

        appDatabase.transactions.upsert(transaction)
        transaction.setError(null)
        return Result.success()
    }

    private fun TransactionEntity.setError(message: String?) {
        transactionState.error = message
        appDatabase.transactions.upsert(this)
    }
}