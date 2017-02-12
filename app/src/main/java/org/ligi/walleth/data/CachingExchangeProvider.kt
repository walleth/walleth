package org.ligi.ewallet.data

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class CachingExchangeProvider(val source: ExchangeRateProvider, var context: Context) : ExchangeRateProvider {

    val sharedPrefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    override fun getExChangeRate(name: String): Double? {

        val key = "EXCHANGE" + name
        if (sharedPrefs.contains(key)) {
            return sharedPrefs.getFloat(key, 0f).toDouble()
        } else {
            val exChangeRate = source.getExChangeRate(name)
            if (exChangeRate != null) {
                sharedPrefs.edit().putFloat(key, exChangeRate.toFloat()).apply()
            }
            return exChangeRate
        }
    }

}