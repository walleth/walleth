package org.walleth.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import org.kethereum.functions.encodeRLP
import org.kethereum.rpc.HttpEthereumRPC
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.ligi.tracedroid.logging.Log
import org.walleth.data.AppDatabase
import org.walleth.data.KEY_TX_HASH
import org.walleth.data.transactions.TransactionEntity
import org.walleth.data.transactions.setHash
import org.walleth.khex.toHexString
import org.walleth.util.getRPCEndpoint

class RelayTransactionWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams), KoinComponent {


    private val okHttpClient: OkHttpClient by inject()
    private val appDatabase: AppDatabase by inject()

    override fun doWork(): Result {

        val txHash = inputData.getString(KEY_TX_HASH)
        val transaction = txHash?.let { appDatabase.transactions.getByHash(it) }

        if (transaction == null) {
            Log.i("Cannot load address with $txHash")
            return Result.failure()
        }

        val chain = transaction.transaction.chain
        val baseURL = chain?.let { appDatabase.chainInfo.getByChainId(it)?.getRPCEndpoint() }

        if (baseURL == null) {
            transaction.setError("RPC url not found for chain $chain")
            return Result.failure()
        } else {
            val rpc = HttpEthereumRPC(baseURL, okHttpClient)


            val result = rpc.sendRawTransaction(transaction.transaction.encodeRLP(transaction.signatureData).toHexString())

            if (result != null) {
                if (result.error?.message != null) {
                    return if (result.error?.message != "Transaction with the same hash was already imported.") {
                        transaction.transactionState.eventLog = transaction.transactionState.eventLog ?: "" + "ERROR: ${result.error?.message}\n"

                        transaction.setError(result.error?.message)
                        appDatabase.transactions.upsert(transaction)

                        Result.failure()
                    } else {
                        Result.success()
                    }


                } else {
                    val newHash = result.result
                    val oldHash = transaction.hash
                    transaction.setHash(if (!newHash.startsWith("0x")) "0x$newHash" else newHash)

                    transaction.transactionState.eventLog = transaction.transactionState.eventLog ?: "" + "relayed"
                    transaction.transactionState.relayed = "via RPC"

                    appDatabase.transactions.deleteByHash(oldHash)
                    appDatabase.transactions.upsert(transaction)
                    transaction.setError(null)
                    return Result.success()
                }
            } else {
                transaction.setError("Could not (yet) relay transaction")
            }
        }

        return Result.retry()
    }

    private fun TransactionEntity.setError(message: String?) {
        transactionState.error = message
        appDatabase.transactions.upsert(this)
    }
}