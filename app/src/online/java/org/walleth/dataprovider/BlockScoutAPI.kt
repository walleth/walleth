package org.walleth.dataprovider


import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.kethereum.functions.getTokenRelevantTo
import org.kethereum.rpc.EthereumRPC
import org.ligi.kaxt.letIf
import org.ligi.tracedroid.logging.Log
import org.walleth.data.AppDatabase
import org.walleth.data.networks.NetworkDefinition
import org.walleth.data.transactions.TransactionEntity
import org.walleth.data.transactions.TransactionState
import org.walleth.kethereum.blockscout.getBlockscoutBaseURL
import java.io.IOException
import java.security.cert.CertPathValidatorException

class BlockScoutAPI(private val appDatabase: AppDatabase,
                    private val okHttpClient: OkHttpClient) {

    private var lastSeenTransactionsBlock = 0L

    fun queryTransactions(addressHex: String, networkDefinition: NetworkDefinition) {
        val rpc = EthereumRPC(networkDefinition.rpcEndpoints.first(), okHttpClient)

        val startBlock = lastSeenTransactionsBlock
        requestList(addressHex, networkDefinition, "txlist", rpc, startBlock)
        requestList(addressHex, networkDefinition, "tokentx", rpc, startBlock)

    }

    private fun requestList(addressHex: String, currentNetwork: NetworkDefinition, action: String, rpc: EthereumRPC, startBlock: Long) {
        val requestString = "module=account&action=$action&address=$addressHex&startblock=$startBlock&sort=asc"

        try {
            val blockScoutResult = getBlockScoutResult(requestString, currentNetwork)
            if (blockScoutResult != null && blockScoutResult.has("result")) {
                val jsonArray = blockScoutResult.getJSONArray("result")
                val newTransactions = parseBlockScoutTransactionList(jsonArray, currentNetwork.chain)

                lastSeenTransactionsBlock = newTransactions.highestBlock

                newTransactions.list.forEach {

                    val oldEntry = appDatabase.transactions.getByHash(it)

                    if (oldEntry == null || oldEntry.transactionState.isPending) {

                        rpc.getTransactionByHash(it)?.let { newTransaction ->

                            val newEntity = TransactionEntity(it,
                                    newTransaction.transaction.getTokenRelevantTo(),
                                    transaction = newTransaction.transaction.copy(
                                            chain = currentNetwork.chain.id.value,
                                            creationEpochSecond = System.currentTimeMillis()
                                    ),
                                    signatureData = newTransaction.signatureData,
                                    transactionState = TransactionState(isPending = false)
                            )
                            appDatabase.transactions.upsert(newEntity)
                        }

                    }

                }

            }
        } catch (e: JSONException) {
            Log.w("Problem with JSON from BlockScout: " + e.message)
        }
    }

    private fun getBlockScoutResult(requestString: String, networkDefinition: NetworkDefinition) = try {
        getBlockScoutResult(requestString, networkDefinition, false)
    } catch (e: CertPathValidatorException) {
        getBlockScoutResult(requestString, networkDefinition, true)
    }

    private fun getBlockScoutResult(requestString: String, networkDefinition: NetworkDefinition, httpFallback: Boolean): JSONObject? {
        val baseURL = getBlockscoutBaseURL(networkDefinition.chain).letIf(httpFallback) {
            replace("https://", "http://") // :-( https://github.com/walleth/walleth/issues/134 )
        }
        val urlString = "$baseURL/api?$requestString"
        val url = Request.Builder().url(urlString).build()
        val newCall: Call = okHttpClient.newCall(url)

        try {
            val resultString = newCall.execute().body().use { it?.string() }
            resultString.let {
                return JSONObject(it)
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        } catch (jsonException: JSONException) {
            jsonException.printStackTrace()
        }

        return null
    }

    private fun reset() {
        lastSeenTransactionsBlock = 0L
    }
}