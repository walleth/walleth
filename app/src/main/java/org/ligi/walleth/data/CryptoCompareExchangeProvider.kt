package org.ligi.walleth.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class CryptoCompareExchangeProvider : ExchangeRateProvider {

    private val okHttpClient by lazy { OkHttpClient.Builder().build() }

    override fun getExChangeRate(name: String): Double? {
        try {
            val request = Request.Builder().url("https://min-api.cryptocompare.com/data/price?fsym=ETH&tsyms=$name").build()
            val response = okHttpClient.newCall(request).execute()
            val string = response.body().string()

            return JSONObject(string).getDouble(name)
        } catch (e: Exception) {
            return null
        }
    }

}