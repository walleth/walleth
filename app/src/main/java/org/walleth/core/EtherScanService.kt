package org.walleth.core

import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Intent
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.kethereum.functions.encodeRLP
import org.kethereum.model.Address
import org.ligi.tracedroid.logging.Log
import org.walleth.BuildConfig
import org.walleth.data.AppDatabase
import org.walleth.data.balances.Balance
import org.walleth.data.balances.upsertIfNewerBlock
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinition
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.isETH
import org.walleth.data.transactions.TransactionEntity
import org.walleth.data.transactions.setHash
import org.walleth.khex.toHexString
import java.io.IOException
import java.math.BigInteger

class EtherScanService : LifecycleService() {

    private val lazyKodein = LazyKodein(appKodein)

    private val okHttpClient: OkHttpClient by lazyKodein.instance()
    private val currentAddressProvider: CurrentAddressProvider by lazyKodein.instance()
    private val tokenProvider: CurrentTokenProvider by lazyKodein.instance()
    private val appDatabase: AppDatabase by lazyKodein.instance()
    private val networkDefinitionProvider: NetworkDefinitionProvider by lazyKodein.instance()


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        var shortcut = false

        appDatabase.transactions.getTransactionsLive().observe(this@EtherScanService, Observer {
            shortcut = true
        })
        currentAddressProvider.observe(this, Observer {
            shortcut = true
        })
        networkDefinitionProvider.observe(this, Observer {
            shortcut = true
        })

        async(CommonPool) {

            while (true) {
                currentAddressProvider.value?.let {
                    tryFetchFromEtherScan(it.hex)
                }

                var i = 0
                while (i < 100 && !shortcut) {
                    delay(100)
                    i++
                }
            }
        }


        relayTransactionsIfNeeded()
        return START_STICKY
    }

    private fun relayTransactionsIfNeeded() {
        appDatabase.transactions.getAllToRelayLive().observe(this, Observer {
            it?.let { it.filter { it.signatureData != null && !it.transactionState.relayedEtherscan }.forEach { relayTransaction(it) } }
        })
    }

    private fun relayTransaction(transaction: TransactionEntity) {
        async(CommonPool) {
            val url = "module=proxy&action=eth_sendRawTransaction&hex=" + transaction.transaction.encodeRLP(transaction.signatureData).toHexString("0x")
            val result = getEtherscanResult(url, networkDefinitionProvider.value!!)

            if (result != null) {
                val oldHash = transaction.hash
                if (result.has("result")) {
                    val newHash = result.getString("result")

                    transaction.setHash(if (!newHash.startsWith("0x")) "0x" + newHash else newHash)
                } else if (result.has("error")) {
                    val error = result.getJSONObject("error")

                    if (error.has("message") &&
                            !error.getString("message").startsWith("known") &&
                            error.getString("message") != "Transaction with the same hash was already imported."
                            ) {
                        transaction.transactionState.error = result.toString()
                    }
                } else {
                    transaction.transactionState.error = result.toString()
                }
                transaction.transactionState.eventLog = transaction.transactionState.eventLog ?: "" + "relayed via EtherScan"
                transaction.transactionState.relayedEtherscan = true

                appDatabase.transactions.deleteByHash(oldHash)
                appDatabase.transactions.upsert(transaction)
            }
        }
    }

    private fun tryFetchFromEtherScan(addressHex: String) {
        queryEtherscanForBalance(addressHex)
        queryTransactions(addressHex)
    }

    private fun queryTransactions(addressHex: String) {
        networkDefinitionProvider.value?.let { currentNetwork ->
            val etherscanResult = getEtherscanResult("module=account&action=txlist&address=$addressHex&startblock=0&endblock=99999999&sort=asc", currentNetwork)
            if (etherscanResult != null) {
                val jsonArray = etherscanResult.getJSONArray("result")
                val newTransactions = parseEtherScanTransactions(jsonArray, currentNetwork.chain)

                newTransactions.forEach {
                    val oldEntry = appDatabase.transactions.getByHash(it.hash)
                    if (oldEntry == null || oldEntry.transactionState.isPending) {
                        appDatabase.transactions.upsert(it)
                    }
                }

            }
        }
    }

    private fun queryEtherscanForBalance(addressHex: String) {

        networkDefinitionProvider.value?.let { currentNetwork ->
            val currentToken = tokenProvider.currentToken
            val etherscanResult = getEtherscanResult("module=proxy&action=eth_blockNumber", currentNetwork)

            if (etherscanResult?.has("result") != true) {
                Log.w("Cannot parse " + etherscanResult)
                return
            }
            val blockNum = etherscanResult.getString("result")?.replace("0x", "")?.toLong(16)

            if (blockNum != null) {
                val balanceString = if (currentToken.isETH()) {
                    getEtherscanResult("module=account&action=balance&address=$addressHex&tag=latest", currentNetwork)?.getString("result")

                } else {
                    getEtherscanResult("module=account&action=tokenbalance&contractaddress=${currentToken.address}&address=$addressHex&tag=latest", currentNetwork)?.getString("result")

                }

                if (balanceString != null) {
                    try {
                        appDatabase.balances.upsertIfNewerBlock(
                                Balance(address = Address(addressHex),
                                        block = blockNum,
                                        balance = BigInteger(balanceString),
                                        tokenAddress = currentToken.address,
                                        chain = currentNetwork.chain
                                )
                        )
                    } catch (e: NumberFormatException) {
                        Log.i("could not parse number " + balanceString)
                    }
                }
            }
        }
    }

    private fun getEtherscanResult(requestString: String, networkDefinition: NetworkDefinition): JSONObject? {
        val baseURL = networkDefinition.getBlockExplorer().baseAPIURL
        val urlString = "$baseURL/api?$requestString&apikey=$" + BuildConfig.ETHERSCAN_APIKEY
        val url = Request.Builder().url(urlString).build()
        val newCall: Call = okHttpClient.newCall(url)

        return try {
            newCall.execute().body().use { it?.string() }.let {
                JSONObject(it)
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            null
        }

    }

}