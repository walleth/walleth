package org.ligi.walleth.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.SystemClock
import okhttp3.*
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.ethereum.geth.Header
import org.ethereum.geth.NewHeadHandler
import org.json.JSONObject
import org.ligi.walleth.App
import org.ligi.walleth.R.id.address
import org.ligi.walleth.data.*
import java.io.IOException
import java.math.BigInteger


class EthereumService : Service() {

    val binder by lazy { Binder() }
    override fun onBind(intent: Intent) = binder
    val okHttpClient = OkHttpClient.Builder().build()!!

    object newHeadHandler : NewHeadHandler {
        override fun onNewHead(p0: Header) {
            App.lastSeenBlock = p0.number
            val address = App.keyStore.accounts[0].address
            val balance = App.ethereumNode.ethereumClient.getBalanceAt(App.ethereumContext, address, App.lastSeenBlock)
            BalanceProvider.setBalance(WallethAddress(address.hex), p0.number, BigInteger(balance.string()))
        }

        override fun onError(p0: String?) {}

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        try {
            App.ethereumNode.start()
        } catch (e: Exception) {
            // TODO better handling - unfortunately ethereumNode does not have one isStarted method which would come handy here
        }

        App.ethereumNode.ethereumClient.subscribeNewHead(App.ethereumContext, newHeadHandler, 16)
        Thread({

            while (true) {
                tryFetchFromEtherScan(App.keyStore.accounts[0].address.hex)
                App.syncProgress = App.ethereumNode.ethereumClient.syncProgress(App.ethereumContext)
                App.bus.post(newBlock)

                TransactionProvider.transactionList.forEach {
                    if (it.ref == Source.WALLETH) {
                        executeTransaction(it)
                    }
                }

                SystemClock.sleep(1000)
            }
        }).start()

        return START_STICKY
    }


    private fun executeTransaction(it: Transaction) {
        it.ref = Source.WALLETH_PROCESSED

        val client = App.ethereumNode.ethereumClient
        val nonceAt = client.getNonceAt(App.ethereumContext, it.from.toGethAddr(), -1)

        val gasPrice = client.suggestGasPrice(App.ethereumContext)

        val gasLimit = BigInt(21_000)

        val newTransaction = Geth.newTransaction(nonceAt, it.to.toGethAddr(), BigInt(it.value.toLong()), gasLimit, gasPrice, ByteArray(0))

        newTransaction.hashCode()
        val accounts = App.keyStore.accounts
        App.keyStore.unlock(accounts.get(0), "default")
        val signHash = App.keyStore.signHash(it.from.toGethAddr(), newTransaction.sigHash.bytes)
        val transactionWithSignature = newTransaction.withSignature(signHash)

        transactionWithSignature.hash.hex
        it.sigHash = newTransaction.sigHash.hex

        client.sendTransaction(App.ethereumContext, transactionWithSignature)
    }

    fun tryFetchFromEtherScan(addressHex: String) {
        queryTransactions(addressHex)
        queryEtherscanForBalance(addressHex)
    }

    fun queryTransactions(addressHex: String) {

        val urlString = "http://rinkeby.etherscan.io/api?module=account&action=txlist&address=$address&startblock=0&endblock=99999999&sort=asc&apikey=$ETHERSCAN_API_TOKEN"
        val url = Request.Builder().url(urlString).build()
        val newCall: Call = okHttpClient.newCall(url)
        newCall.enqueueOnlySuccess {
            //success(it.toString())
        }

    }

    fun queryEtherscanForBalance(addressHex: String) {
        val urlString = "https://rinkeby.etherscan.io/api?module=account&action=balance&address=$addressHex&tag=latest&apikey=$ETHERSCAN_API_TOKEN"
        val url = Request.Builder().url(urlString).build()
        val newCall: Call = okHttpClient.newCall(url)
        newCall.enqueueOnlySuccess {
            //success(BigInteger(it.getString("result")))
        }

    }

    fun Call.enqueueOnlySuccess(success: (it: JSONObject) -> Unit) {
        enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call?, response: Response) {
                /*val string = JSONObject(response.body().string())
                if (string.getString("message") == "OK") {
                    success(string)
                }
                */
            }
        })
    }


}
