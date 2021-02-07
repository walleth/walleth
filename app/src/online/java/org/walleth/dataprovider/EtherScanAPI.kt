package org.walleth.dataprovider


import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.kethereum.extensions.transactions.getTokenRelevantTo
import org.kethereum.model.ChainId
import org.kethereum.rpc.EthereumRPC
import org.ligi.kaxt.letIf
import org.walleth.data.AppDatabase
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.data.rpc.RPCProvider
import org.walleth.data.transactions.TransactionEntity
import org.walleth.data.transactions.TransactionState
import org.walleth.kethereum.etherscan.getEtherScanAPIBaseURL
import timber.log.Timber
import java.io.IOException
import java.security.cert.CertPathValidatorException

class EtherScanAPI(private val appDatabase: AppDatabase,
                   private val rpcProvider: RPCProvider,
                   private val okHttpClient: OkHttpClient) {

    private var lastSeenTransactionsBlock = 0L

    suspend fun queryTransactions(addressHex: String, networkDefinition: ChainInfo) {
        rpcProvider.getForChain(ChainId(networkDefinition.chainId))?.let {rpc ->
            val startBlock = lastSeenTransactionsBlock
            requestList(addressHex, networkDefinition, "txlist", rpc, startBlock)
            requestList(addressHex, networkDefinition, "tokentx", rpc, startBlock)
        }
    }

    private fun requestList(addressHex: String, currentChain: ChainInfo, action: String, rpc: EthereumRPC, startBlock: Long) {
        val requestString = "module=account&action=$action&address=$addressHex&startblock=$startBlock&sort=asc"

        try {
            val result = getEtherscanResult(requestString, currentChain)
            if (result != null && result.has("result")) {
                val jsonArray = result.getJSONArray("result")
                val newTransactions = parseEtherscanTransactionList(jsonArray)

                lastSeenTransactionsBlock = newTransactions.highestBlock

                newTransactions.list.forEach {

                    val oldEntry = appDatabase.transactions.getByHash(it)

                    if (oldEntry == null || oldEntry.transactionState.isPending) {

                        rpc.getTransactionByHash(it)?.let { newTransaction ->

                            val newEntity = TransactionEntity(it,
                                    newTransaction.transaction.getTokenRelevantTo(),
                                    transaction = newTransaction.transaction.copy(
                                            chain = currentChain.chainId,
                                            creationEpochSecond = System.currentTimeMillis() / 1000
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
            Timber.w("Problem with JSON from EtherScan: %s", e.message)
        }
    }

    private fun getEtherscanResult(requestString: String, chainInfo: ChainInfo) = try {
        getEtherscanResult(requestString, chainInfo, false)
    } catch (e: CertPathValidatorException) {
        getEtherscanResult(requestString, chainInfo, true)
    }

    private fun getEtherscanResult(requestString: String, chainInfo: ChainInfo, httpFallback: Boolean): JSONObject? {
        val baseURL = getEtherScanAPIBaseURL(ChainId(chainInfo.chainId)).letIf(httpFallback) {
            replace("https://", "http://") // :-( https://github.com/walleth/walleth/issues/134 )
        }
        val urlString = "$baseURL/api?$requestString"
        val request = Request.Builder().url(urlString).build()
        val newCall: Call = okHttpClient.newCall(request)

        try {
            val resultString = newCall.execute().body().use { it?.string() }
            resultString?.let {
                return JSONObject(resultString)
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        } catch (jsonException: JSONException) {
            jsonException.printStackTrace()
        }

        return null
    }

}