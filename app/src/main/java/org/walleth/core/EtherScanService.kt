package org.walleth.core

import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Binder
import android.os.SystemClock
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import okhttp3.*
import org.json.JSONObject
import org.kethereum.functions.encodeRLP
import org.kethereum.model.Address
import org.walleth.BuildConfig
import org.walleth.data.AppDatabase
import org.walleth.data.balances.Balance
import org.walleth.data.balances.upsertIfNewerBlock
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.BaseCurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.isETH
import org.walleth.data.transactions.TransactionProvider
import org.walleth.data.transactions.TransactionWithState
import org.walleth.khex.toHexString
import org.walleth.ui.ChangeObserver
import java.io.IOException
import java.math.BigInteger

class EtherScanService : LifecycleService() {

    private val binder by lazy { Binder() }

    override fun onBind(intent: Intent): Binder {
        super.onBind(intent)
        return binder
    }

    val lazyKodein = LazyKodein(appKodein)

    val okHttpClient: OkHttpClient by lazyKodein.instance()
    val keyStore: WallethKeyStore by lazyKodein.instance()
    val transactionProvider: TransactionProvider by lazyKodein.instance()
    val currentAddressProvider: BaseCurrentAddressProvider by lazyKodein.instance()
    val tokenProvider: CurrentTokenProvider by lazyKodein.instance()
    val appDatabase: AppDatabase by lazyKodein.instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by lazyKodein.instance()

    var shortcut = false
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val shortcutChangeObserver: ChangeObserver = object : ChangeObserver {
            override fun observeChange() {
                shortcut = true
            }
        }

        transactionProvider.registerChangeObserver(shortcutChangeObserver)
        currentAddressProvider.observe(this, Observer {
            shortcut = true
        })
        networkDefinitionProvider.observe(this, Observer {
            shortcut = true
        })

        Thread({

            while (true) {
                val currentAddress = currentAddressProvider.value

                if (currentAddress != null) {
                    tryFetchFromEtherScan(currentAddress.hex)

                    relayTransactionsIfNeeded()
                }
                var i = 0
                while (i < 100 && !shortcut) {
                    SystemClock.sleep(100)
                    i++
                }
            }
        }).start()


        return START_STICKY
    }

    private fun relayTransactionsIfNeeded() {
        transactionProvider.getAllTransactions().forEach {
            if (it.transaction.signatureData != null) {
                relayTransaction(it)
            }
        }
    }

    private fun relayTransaction(transaction: TransactionWithState) {

        val url = "module=proxy&action=eth_sendRawTransaction&hex=" + transaction.transaction.encodeRLP().toHexString("")
        val result = getEtherscanResult(url)

        if (result != null) {
            if (result.has("result")) {
                transaction.transaction.txHash = result.getString("result")
            } else if (result.has("error")) {
                val error = result.getJSONObject("error")

                if (error.has("message") &&
                        !error.getString("message").startsWith("known") &&
                        error.getString("message") != "Transaction with the same hash was already imported."
                        ) {
                    transaction.state.error = result.toString()
                }
            } else {
                transaction.state.error = result.toString()
            }
            transaction.state.eventLog = transaction.state.eventLog ?: "" + "relayed via EtherScan"
            transaction.state.relayedEtherscan = true
        }
    }

    fun tryFetchFromEtherScan(addressHex: String) {
        queryEtherscanForBalance(addressHex)
        queryTransactions(addressHex)
    }

    private fun queryTransactions(addressHex: String) {
        val etherscanResult = getEtherscanResult("module=account&action=txlist&address=$addressHex&startblock=0&endblock=99999999&sort=asc")
        if (etherscanResult != null) {
            val jsonArray = etherscanResult.getJSONArray("result")
            val transactions = parseEtherScanTransactions(jsonArray)
            transactionProvider.addTransactions(transactions)
        }
    }

    fun queryEtherscanForBalance(addressHex: String) {

        val currentToken = tokenProvider.currentToken
        val blockNum = getEtherscanResult("module=proxy&action=eth_blockNumber")?.getString("result")?.replace("0x", "")?.toLong(16)

        if (blockNum != null) {
            val balanceString =if (currentToken.isETH()) {
                 getEtherscanResult("module=account&action=balance&address=$addressHex&tag=latest")?.getString("result")

            } else {
                getEtherscanResult("module=account&action=tokenbalance&contractaddress=${currentToken.address}&address=$addressHex&tag=latest")?.getString("result")

            }

            if (balanceString != null) {
                appDatabase.balances.upsertIfNewerBlock(
                        Balance(address = Address(addressHex),
                                block = blockNum,
                                balance = BigInteger(balanceString),
                                tokenAddress = currentToken.address,
                                chain = networkDefinitionProvider.getCurrent().chain
                                )
                )
            }
        }
    }

    fun Call.enqueueOnlySuccess(success: (it: JSONObject) -> Unit) {
        enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call?, response: Response) {
                response.body()?.let { it.use { success(JSONObject(it.string())) } }
            }
        })
    }


    fun getEtherscanResult(requestString: String): JSONObject? {
        val baseURL = networkDefinitionProvider.value!!.getBlockExplorer().baseAPIURL
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