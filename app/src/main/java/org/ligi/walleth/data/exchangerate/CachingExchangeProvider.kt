package org.ligi.walleth.data.exchangerate

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import org.ligi.walleth.data.exchangerate.ExchangeRateProvider
import java.math.BigDecimal

class CachingExchangeProvider(val source: ExchangeRateProvider, var context: Context) : ExchangeRateProvider {

    val sharedPrefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    override fun getExChangeRate(name: String) : BigDecimal? {

        val key = "EX" + name
        if (sharedPrefs.contains(key)) {
            return BigDecimal(sharedPrefs.getString(key, "0.0"))
        } else {
            val exChangeRate = source.getExChangeRate(name)
            if (exChangeRate != null) {
                sharedPrefs.edit().putString(key, exChangeRate.toString()).apply()
            }
            return exChangeRate
        }
    }

}