package org.walleth.data.config

import android.preference.PreferenceManager
import android.support.v7.app.AppCompatDelegate
import com.chibatching.kotpref.KotprefModel
import org.walleth.R

object KotprefSettings : KotprefModel(), Settings {


    override var currentFiat by stringPref(default = "USD")

    internal val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    override fun getStatsName() = sharedPreferences.getString(context.getString(R.string.key_prefs_stats_username),context.getString(R.string.default_stats_username))

    override fun getNightMode()
            = when (sharedPreferences.getString(context.getString(R.string.key_prefs_day_night), context.getString(R.string.default_day_night))) {
        "day" -> AppCompatDelegate.MODE_NIGHT_NO
        "night" -> AppCompatDelegate.MODE_NIGHT_YES
        "auto" -> AppCompatDelegate.MODE_NIGHT_AUTO
        else -> AppCompatDelegate.MODE_NIGHT_AUTO
    }

}
