package org.walleth.data.config

import android.preference.PreferenceManager
import android.support.v7.app.AppCompatDelegate
import com.chibatching.kotpref.KotprefModel
import org.walleth.R
import org.walleth.data.networks.RINKEBY_CHAIN_ID
import java.math.BigInteger
import java.security.SecureRandom

object KotprefSettings : KotprefModel(), Settings {

    override var currentFiat by stringPref(default = "USD")
    override var startupWarningDone by booleanPref(default = false)

    override var chain by longPref(RINKEBY_CHAIN_ID)
    override var accountAddress by nullableStringPref(null)
    override var addressBookInitialized by booleanPref(false)

    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    private fun createRandomUsername()
            = context.getString(R.string.default_stats_username) + " " + BigInteger(130, SecureRandom()).toString(32).substring(0, 5)

    override fun getStatsName() = sharedPreferences.getString(context.getString(R.string.key_prefs_stats_username), createRandomUsername())

    override fun isLightClientWanted() = sharedPreferences.getBoolean(context.getString(R.string.key_prefs_start_light), false)

    override fun getNightMode()
            = when (sharedPreferences.getString(context.getString(R.string.key_prefs_day_night), context.getString(R.string.default_day_night))) {
        "day" -> AppCompatDelegate.MODE_NIGHT_NO
        "night" -> AppCompatDelegate.MODE_NIGHT_YES
        "auto" -> AppCompatDelegate.MODE_NIGHT_AUTO
        else -> AppCompatDelegate.MODE_NIGHT_AUTO
    }

}
