package org.walleth.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.SystemClock
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import okhttp3.*
import org.json.JSONObject
import org.kethereum.functions.toHexString
import org.kethereum.model.Address
import org.walleth.BuildConfig
import org.walleth.data.BalanceProvider
import org.walleth.data.exchangerate.ETH_TOKEN
import org.walleth.data.exchangerate.TokenProvider
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.transactions.TransactionProvider
import org.walleth.data.transactions.TransactionWithState
import org.walleth.ui.ChangeObserver
import java.io.IOException
import java.lang.NumberFormatException
import java.math.BigInteger

class EtherScanService : Service() {

    val binder by lazy { Binder() }
    override fun onBind(intent: Intent) = binder


    val lazyKodein = LazyKodein(appKodein)

    val okHttpClient: OkHttpClient by lazyKodein.instance()
    val keyStore: WallethKeyStore by lazyKodein.instance()
    val transactionProvider: TransactionProvider by lazyKodein.instance()
    val balanceProvider: BalanceProvider by lazyKodein.instance()
    val tokenProvider: TokenProvider by lazyKodein.instance()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Thread({

            var shortcut = false

            transactionProvider.registerChangeObserver(object : ChangeObserver {
                override fun observeChange() {
                    shortcut = true
                }

            })

            keyStore.registerChangeObserver(object : ChangeObserver {
                override fun observeChange() {
                    shortcut = true
                }

            })

            while (true) {
                tryFetchFromEtherScan(keyStore.getCurrentAddress().hex)

                relayTransactionsIfNeeded()

                var i = 0
                while (i < 10000 && !shortcut) {
                    SystemClock.sleep(1)
                    i++
                }

                shortcut = false
            }
        }).start()


        return START_STICKY
    }

    private fun relayTransactionsIfNeeded() {
        transactionProvider.getAllTransactions().forEach {
            if (it.transaction.signedRLP != null) {
                relayTransaction(it)
            }
        }
    }

    private fun relayTransaction(transaction: TransactionWithState) {
        transaction.transaction.signedRLP?.let {
            getEtherscanResult("module=proxy&action=eth_sendRawTransaction&hex=" + it.fold("0x", { s: String, byte: Byte -> s + byte.toHexString() })) {
                if (it.has("result")) {
                    transaction.transaction.txHash = it.getString("result")
                } else {
                    if (!it.toString().startsWith("known")) {
                        transaction.state.error = it.toString()
                    }
                }
                transaction.state.eventLog = transaction.state.eventLog ?: "" + "relayed via EtherScan"
                transaction.transaction.signedRLP = null
            }
        }
    }

    fun tryFetchFromEtherScan(addressHex: String) {
        queryEtherscanForBalance(addressHex)
        queryTransactions(addressHex)
    }

    fun queryTransactions(addressHex: String) {

        getEtherscanResult("module=account&action=txlist&address=$addressHex&startblock=0&endblock=99999999&sort=asc") {

            val jsonArray = it.getJSONArray("result")
            val transactions = parseEtherScanTransactions(jsonArray)
            transactionProvider.addTransactions(transactions)
        }

    }

    fun queryEtherscanForBalance(addressHex: String) {

        val currentToken = tokenProvider.currentToken
        if (currentToken == ETH_TOKEN) {
            getEtherscanResult("module=account&action=balance&address=$addressHex&tag=latest") {
                val balance = BigInteger(it.getString("result"))
                getEtherscanResult("module=proxy&action=eth_blockNumber") {
                    balanceProvider.setBalance(Address(addressHex), it.getString("result").replace("0x", "").toLong(16), balance, ETH_TOKEN)
                }
            }
        } else {
            getEtherscanResult("module=account&action=tokenbalance&contractaddress=${currentToken.address}&address=$addressHex&tag=latest") {
                try {
                    val balance = BigInteger(it.getString("result"))
                    getEtherscanResult("module=proxy&action=eth_blockNumber") {
                        balanceProvider.setBalance(Address(addressHex), it.getString("result").replace("0x", "").toLong(16), balance, currentToken)
                    }
                } catch(e: NumberFormatException) {}
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


    fun getEtherscanResult(requestString: String, successCallback: (responseJSON: JSONObject) -> Unit) {
        val urlString = "https://rinkeby.etherscan.io/api?$requestString&apikey=$" + BuildConfig.ETHERSCAN_APIKEY
        val url = Request.Builder().url(urlString).build()
        val newCall: Call = okHttpClient.newCall(url)
        newCall.enqueueOnlySuccess(successCallback)
    }
}
