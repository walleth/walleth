package org.walleth.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import org.kethereum.functions.encodeRLP
import org.kethereum.model.ChainId
import org.kethereum.rpc.EthereumRPC
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.walleth.data.AppDatabase
import org.walleth.data.KEY_TX_HASH
import org.walleth.data.networks.findNetworkDefinition
import org.walleth.data.transactions.setHash
import org.walleth.khex.toHexString
import timber.log.Timber

class RelayTransactionWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams), KoinComponent {


    private val okHttpClient: OkHttpClient by inject()
    private val appDatabase: AppDatabase by inject()

    override fun doWork(): Result {

        val txHash = inputData.getString(KEY_TX_HASH)
        val transaction = txHash?.let { appDatabase.transactions.getByHash(it) }

        if (transaction == null) {
            Timber.i("Cannot load address with $txHash")
            return Result.failure()
        }

        val chain = transaction.transaction.chain?.let { ChainId(it) }
        val baseURL = chain?.findNetworkDefinition()?.rpcEndpoints?.firstOrNull()

        if (baseURL == null) {
            transaction.transactionState.error = "RPC url not found for chain $chain"
        } else {
            val rpc = EthereumRPC(baseURL, okHttpClient)


            val result = rpc.sendRawTransaction(transaction.transaction.encodeRLP(transaction.signatureData).toHexString())

            if (result != null) {
                if (result.error?.message != null) {
                    if (result.error?.message != "Transaction with the same hash was already imported.") {
                        // this error should not be surfaced to user
                        transaction.transactionState.error = result.error?.message;
                        transaction.transactionState.eventLog = transaction.transactionState.eventLog ?: "" + "ERROR: ${result.error?.message}\n"

                        appDatabase.transactions.upsert(transaction);

                        return Result.failure()
                    }
                } else {
                    val newHash = result.result
                    val oldHash = transaction.hash
                    transaction.setHash(if (!newHash.startsWith("0x")) "0x$newHash" else newHash)

                    transaction.transactionState.eventLog = transaction.transactionState.eventLog ?: "" + "relayed via ${rpc.baseURL}"
                    transaction.transactionState.relayed = rpc.baseURL

                    appDatabase.transactions.deleteByHash(oldHash)
                    appDatabase.transactions.upsert(transaction)
                }
            }
        }

        return Result.success()
    }
}