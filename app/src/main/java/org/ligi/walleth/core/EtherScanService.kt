package org.ligi.walleth.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import okhttp3.*
import org.json.JSONObject
import org.ligi.walleth.R.id.address
import org.ligi.walleth.data.ETHERSCAN_API_TOKEN
import java.io.IOException


class EtherScanService : Service() {

    val binder by lazy { Binder() }
    override fun onBind(intent: Intent) = binder
    val okHttpClient = OkHttpClient.Builder().build()!!


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_STICKY
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
