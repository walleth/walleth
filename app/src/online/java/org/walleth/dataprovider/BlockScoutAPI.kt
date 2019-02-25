package org.walleth.dataprovider


import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.ligi.kaxt.letIf
import org.ligi.tracedroid.logging.Log
import org.walleth.data.AppDatabase
import org.walleth.data.networks.NetworkDefinition
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.kethereum.blockscout.getBlockscoutBaseURL
import java.io.IOException
import java.security.cert.CertPathValidatorException

class BlockScoutAPI(private val networkDefinitionProvider: NetworkDefinitionProvider,
                    private val appDatabase: AppDatabase,
                    private val okHttpClient: OkHttpClient) {

    private var lastSeenTransactionsBlock = 0L

    fun queryTransactions(addressHex: String) {
        networkDefinitionProvider.value?.let { currentNetwork ->
            val requestString = "module=account&action=txlist&address=$addressHex&startblock=$lastSeenTransactionsBlock&sort=asc"

            try {
                val blockScoutResult = getBlockScoutResult(requestString, currentNetwork)
                if (blockScoutResult != null && blockScoutResult.has("result")) {
                    val jsonArray = blockScoutResult.getJSONArray("result")
                    val newTransactions = parseBlockScoutTransactionList(jsonArray, currentNetwork.chain)

                    lastSeenTransactionsBlock = newTransactions.highestBlock

                    newTransactions.list.forEach {

                        val oldEntry = appDatabase.transactions.getByHash(it.hash)
                        if (oldEntry == null || oldEntry.transactionState.isPending) {
                            appDatabase.transactions.upsert(it)
                        }
                    }

                }
            } catch (e: JSONException) {
                Log.w("Problem with JSON from BlockScout: " + e.message)
            }
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