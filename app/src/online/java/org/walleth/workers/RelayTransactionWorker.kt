package org.walleth.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import org.kethereum.functions.encodeRLP
import org.kethereum.rpc.EthereumRPCException
import org.kethereum.rpc.HttpEthereumRPC
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.komputing.khex.extensions.toHexString
import org.ligi.tracedroid.logging.Log
import org.walleth.data.AppDatabase
import org.walleth.data.KEY_TX_HASH
import org.walleth.data.transactions.TransactionEntity
import org.walleth.data.transactions.setHash
import org.walleth.util.getRPCEndpoint

class RelayTransactionWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams), KoinComponent {


    private val okHttpClient: OkHttpClient by inject()
    private val appDatabase: AppDatabase by inject()

    override suspend fun doWork(): Result {

        val txHash = inputData.getString(KEY_TX_HASH)
        val transaction: TransactionEntity? = txHash?.let { appDatabase.transactions.getByHash(it) }

        if (transaction == null) {
            Log.i("Cannot load address with $txHash")
            return Result.failure()
        }

        val chain = transaction.transaction.chain
        val baseURL = chain?.let { appDatabase.chainInfo.getByChainId(it)?.getRPCEndpoint() }

        if (baseURL == null) {
            transaction.setError("RPC url not found for chain $chain")
            return Result.failure()
        }

        val rpc = HttpEthereumRPC(baseURL, okHttpClient)

        try {
            val result = rpc.sendRawTransaction(transaction.transaction.encodeRLP(transaction.signatureData).toHexString())

            return if (result != null) {
                val oldHash = transaction.hash
                transaction.setHash(if (!result.startsWith("0x")) "0x$result" else result)

                transaction.transactionState.eventLog = transaction.transactionState.eventLog ?: "" + "relayed"
                transaction.transactionState.relayed = "via RPC"

                appDatabase.transactions.deleteByHash(oldHash)
                appDatabase.transactions.upsert(transaction)
                transaction.setError(null)
                Result.success()
            } else {
                transaction.setError("Could not (yet) relay transaction")
                Result.retry()
            }
        } catch (e: EthereumRPCException) {
            return if (e.message == "Transaction with the same hash was already imported.") {
                Result.success()
            } else {
                transaction.transactionState.eventLog = transaction.transactionState.eventLog ?: "" + "ERROR: ${e.message}\n"

                transaction.setError(e.message)
                appDatabase.transactions.upsert(transaction)

                Result.failure()
            }
        }
    }

    private fun TransactionEntity.setError(message: String?) {
        transactionState.error = message
        appDatabase.transactions.upsert(this)
    }
}