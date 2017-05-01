package org.ligi.walleth.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.SystemClock
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import okhttp3.*
import org.json.JSONObject
import org.ligi.walleth.data.BalanceProvider
import org.ligi.walleth.data.ETHERSCAN_API_TOKEN
import org.ligi.walleth.data.WallethAddress
import org.ligi.walleth.data.keystore.WallethKeyStore
import org.ligi.walleth.data.transactions.Transaction
import org.ligi.walleth.data.transactions.TransactionProvider
import org.ligi.walleth.data.transactions.TransactionSource
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import java.io.IOException
import java.math.BigInteger


class EtherScanService : Service() {

    val binder by lazy { Binder() }
    override fun onBind(intent: Intent) = binder


    val lazyKodein = LazyKodein(appKodein)

    val okHttpClient: OkHttpClient by lazyKodein.instance()
    val keyStore: WallethKeyStore by lazyKodein.instance()
    val transactionProvider: TransactionProvider by lazyKodein.instance()
    val balanceProvider: BalanceProvider by lazyKodein.instance()


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Thread({

            while (true) {
                tryFetchFromEtherScan(keyStore.getCurrentAddress().hex)

                SystemClock.sleep(10000)
            }
        }).start()


        return START_STICKY
    }

    fun tryFetchFromEtherScan(addressHex: String) {
        queryTransactions(addressHex)
        queryEtherscanForBalance(addressHex)
    }

    fun queryTransactions(addressHex: String) {

        val urlString = "http://rinkeby.etherscan.io/api?module=account&action=txlist&address=$addressHex&startblock=0&endblock=99999999&sort=asc&apikey=$ETHERSCAN_API_TOKEN"
        val url = Request.Builder().url(urlString).build()
        val newCall: Call = okHttpClient.newCall(url)
        newCall.enqueueOnlySuccess {

            val jsonArray = it.getJSONArray("result")
            (0..(jsonArray.length() - 1)).forEach {
                val transactionJson = jsonArray.getJSONObject(it)
                val value = BigInteger(transactionJson.getString("value"))
                val timeStamp = Instant.ofEpochSecond(transactionJson.getString("timeStamp").toLong())
                val ofInstant = LocalDateTime.ofInstant(timeStamp, ZoneOffset.systemDefault())
                val transaction = Transaction(
                        value,
                        WallethAddress(transactionJson.getString("from")),
                        WallethAddress(transactionJson.getString("to")),
                        ref = TransactionSource.ETHERSCAN,
                        txHash = transactionJson.getString("hash"),
                        localTime = ofInstant
                )
                transactionProvider.addTransaction(transaction)
            }
        }

    }

    fun queryEtherscanForBalance(addressHex: String) {

        val urlString = "https://rinkeby.etherscan.io/api?module=account&action=balance&address=$addressHex&tag=latest&apikey=$ETHERSCAN_API_TOKEN"
        val url = Request.Builder().url(urlString).build()
        val newCall: Call = okHttpClient.newCall(url)
        newCall.enqueueOnlySuccess {
            balanceProvider.setBalance(WallethAddress(addressHex),1,BigInteger(it.getString("result")))

            // TODO get block number https://rinkeby.etherscan.io/api?module=proxy&action=eth_blockNumber&apikey=YourApiKeyToken
        }

    }

    fun Call.enqueueOnlySuccess(success: (it: JSONObject) -> Unit) {
        enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call?, response: Response) {
                val jsonObject = JSONObject(response.body().string())
                if (jsonObject.getString("message") == "OK") {
                    success(jsonObject)
                }
            }
        })
    }


}
