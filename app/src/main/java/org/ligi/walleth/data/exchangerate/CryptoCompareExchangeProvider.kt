package org.ligi.walleth.data.exchangerate

import android.content.Context
import okhttp3.*
import okio.Okio
import org.json.JSONObject
import org.threeten.bp.LocalTime
import java.io.File
import java.io.IOException
import java.math.BigDecimal

class CryptoCompareExchangeProvider(context: Context,val okHttpClient: OkHttpClient) : BaseExchangeProvider() {


    override fun addFiat(name: String) {
        super.addFiat(name)
        refresh()
    }

    private val lastDataFile = File(context.cacheDir, "exchangerates.json")

    init {
        if (lastDataFile.exists()) {
            setFromFile()
        } else {
            fiatInfoMap.putAll(mapOf(
                    "EUR" to FiatInfo("EUR"),
                    "NZD" to FiatInfo("NZD"),
                    "CHF" to FiatInfo("CHF"),
                    "USD" to FiatInfo("USD"))
            )
        }
        refresh()
    }

    private fun setFromFile() {
        val json = JSONObject(Okio.buffer(Okio.source(lastDataFile)).readUtf8())
        json.keys().forEach {
            fiatInfoMap.put(it, FiatInfo(it, "", LocalTime.now(), BigDecimal(json.getDouble(it))))
        }
    }

    fun refresh() {
        val names = fiatInfoMap.keys.joinToString(",")
        val request = Request.Builder().url("https://min-api.cryptocompare.com/data/price?fsym=ETH&tsyms=$names").build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {}

            override fun onResponse(call: Call?, response: Response) {
                if (response.code() == 200) {
                    val responseBody = response.body()
                    val bufferedSink = Okio.buffer(Okio.sink(lastDataFile))
                    bufferedSink.writeAll(responseBody.source())
                    bufferedSink.close()
                    responseBody.close()
                    setFromFile()
                }
            }

        })

    }


}