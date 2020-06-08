package org.walleth.data.config

import android.content.SharedPreferences
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.chibatching.kotpref.KotprefModel
import org.walleth.R
import org.walleth.data.DEFAULT_GAS_PRICE
import org.walleth.util.asBigDecimal
import java.math.BigInteger

object KotprefSettings : KotprefModel(), Settings {

    override var currentFiat by stringPref(default = "USD")
    override var onboardingDone by booleanPref(default = false)
    override var showOnlyStaredTokens by booleanPref(default = false)
    override var showOnlyTokensOnCurrentNetwork by booleanPref(default = false)

    override var filterAddressesStared by booleanPref(default = false)
    override var filterAddressesKeyOnly by booleanPref(default = false)
    override var filterFaucet by booleanPref(default = false)
    override var filterFastFaucet by booleanPref(default = false)
    override var filterTincubeth by booleanPref(default = false)
    override var logRPCRequests: Boolean by booleanPref(default = false)
    override var chain by longPref(5L)
    override var accountAddress by nullableStringPref(null)

    override var addressInitVersion by intPref(0)
    override var tokensInitVersion by intPref(0)
    override var dataVersion by intPref(0)

    override var currentGoVerbosity by intPref(3)

    override var toolbarBackgroundColor by intPref(ContextCompat.getColor(context, R.color.colorInitialToolbar))
    override var toolbarForegroundColor by intPref(Color.BLACK)

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun isLightClientWanted() = sharedPreferences.getBoolean(context.getString(R.string.key_prefs_start_light), false)

    override fun getNightMode() = when (sharedPreferences.getString(context.getString(R.string.key_prefs_day_night), context.getString(R.string.default_day_night))) {
        "day" -> AppCompatDelegate.MODE_NIGHT_NO
        "night" -> AppCompatDelegate.MODE_NIGHT_YES
        "auto" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }


    override fun isScreenshotsDisabled() = sharedPreferences.getBoolean(context.getString(R.string.key_noscreenshots), false)
    override fun isAdvancedFunctionsEnabled() = sharedPreferences.getBoolean(context.getString(R.string.key_advanced_functions), false)

    override fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = preferences.registerOnSharedPreferenceChangeListener(listener)
    override fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = preferences.unregisterOnSharedPreferenceChangeListener(listener)

    override fun getGasPriceFor(chainId: BigInteger): BigInteger {
        val gasPrice = sharedPreferences.getString("KEY_GAS_PRICE" + chainId, null)
        return gasPrice?.asBigDecimal()?.toBigInteger() ?: DEFAULT_GAS_PRICE
    }

    override fun storeGasPriceFor(gasPrice: BigInteger, chainId: BigInteger) {
        sharedPreferences.edit()
                .putString("KEY_GAS_PRICE" + chainId, gasPrice.toString())
                .apply()
    }
}
