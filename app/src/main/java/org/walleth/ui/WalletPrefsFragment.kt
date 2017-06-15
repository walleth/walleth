package org.walleth.ui


import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.PreferenceFragmentCompat
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.ligi.kaxt.recreateWhenPossible
import org.walleth.App
import org.walleth.R
import org.walleth.core.GethLightEthereumService
import org.walleth.core.GethLightEthereumService.Companion.gethStopIntent
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.TokenProvider

class WalletPrefsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    val settings: Settings by LazyKodein(appKodein).instance()
    val tokenProvider: TokenProvider by LazyKodein(appKodein).instance()

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        findPreference(getString(R.string.key_reference)).summary = "Currently: " + settings.currentFiat
        findPreference(getString(R.string.key_token)).summary = "Currently: " + tokenProvider.currentToken.name

        setUserNameSummary()
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.key_prefs_day_night)) {

            App.applyNightMode(settings)
            activity.recreateWhenPossible()
        }
        if (key == getString(R.string.key_prefs_start_light)) {
            if ((findPreference(key) as CheckBoxPreference).isChecked != GethLightEthereumService.isRunning) {
                if (GethLightEthereumService.isRunning) {
                    context.startService(context.gethStopIntent())
                } else {
                    context.startService(Intent(context, GethLightEthereumService::class.java))
                }
            }

        }
        setUserNameSummary()

    }

    private fun setUserNameSummary() {
        findPreference(getString(R.string.key_prefs_stats_username)).summary = settings.getStatsName() + " @ https://stats.rinkeby.io"
    }

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

}