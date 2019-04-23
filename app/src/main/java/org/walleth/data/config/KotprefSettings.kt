package org.walleth.data.config

import android.content.SharedPreferences
import android.graphics.Color
import android.preference.PreferenceManager
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatDelegate
import com.chibatching.kotpref.KotprefModel
import org.walleth.R
import org.walleth.data.DEFAULT_GAS_PRICE
import org.walleth.data.networks.NetworkDefinition
import org.walleth.functions.asBigDecimal
import java.math.BigInteger
import java.security.SecureRandom

object KotprefSettings : KotprefModel(), Settings {

    override var currentFiat by stringPref(default = "USD")
    override var onboardingDone by booleanPref(default = false)
    override var showOnlyStaredTokens by booleanPref(default = false)
    override var showOnlyTokensOnCurrentNetwork by booleanPref(default = false)

    override var filterAddressesStared by booleanPref(default = false)
    override var filterAddressesKeyOnly by booleanPref(default = false)

    override var chain by longPref(5L)
    override var accountAddress by nullableStringPref(null)

    override var addressInitVersion by intPref(0)
    override var tokensInitVersion by intPref(0)
    override var dataVersion by intPref(0)

    override var currentGoVerbosity by intPref(3)

    override var toolbarBackgroundColor by intPref(ContextCompat.getColor(context, R.color.colorPrimary))
    override var toolbarForegroundColor by intPref(Color.BLACK)

    override var showDebug by booleanPref(default = false)

    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    private fun createRandomUsername() = context.getString(R.string.default_stats_username) + " " + BigInteger(130, SecureRandom()).toString(32).substring(0, 5)

    override fun getStatsName(): String {
        val key = context.getString(R.string.key_prefs_stats_username)
        val string = sharedPreferences.getString(key, null)
        if (string != null) {
            return string
        }
        val newName = createRandomUsername()
        sharedPreferences.edit().putString(key, newName).apply()
        return newName
    }

    override fun isLightClientWanted() = sharedPreferences.getBoolean(context.getString(R.string.key_prefs_start_light), false)

    override fun getNightMode() = when (sharedPreferences.getString(context.getString(R.string.key_prefs_day_night), context.getString(R.string.default_day_night))) {
        "day" -> AppCompatDelegate.MODE_NIGHT_NO
        "night" -> AppCompatDelegate.MODE_NIGHT_YES
        "auto" -> AppCompatDelegate.MODE_NIGHT_AUTO
        else -> AppCompatDelegate.MODE_NIGHT_AUTO
    }


    override fun isScreenshotsDisabled() = sharedPreferences.getBoolean(context.getString(R.string.key_noscreenshots), false)

    override fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = preferences.registerOnSharedPreferenceChangeListener(listener)
    override fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = preferences.unregisterOnSharedPreferenceChangeListener(listener)

    override fun getGasPriceFor(current: NetworkDefinition): BigInteger {
        val gasPrice = sharedPreferences.getString("KEY_GAS_PRICE" + current.chain.id, null)
        return gasPrice?.asBigDecimal()?.toBigInteger() ?: DEFAULT_GAS_PRICE
    }

    override fun storeGasPriceFor(gasPrice: BigInteger, network: NetworkDefinition) {
        sharedPreferences.edit()
                .putString("KEY_GAS_PRICE" + network.chain.id, gasPrice.toString())
                .apply()
    }
}
