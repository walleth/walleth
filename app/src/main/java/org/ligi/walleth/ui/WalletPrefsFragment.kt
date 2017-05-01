package org.ligi.walleth.ui


import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.ligi.kaxt.recreateWhenPossible
import org.ligi.walleth.App
import org.ligi.walleth.R
import org.ligi.walleth.data.config.Settings

class WalletPrefsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    val settings: Settings by LazyKodein(appKodein).instance()

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        findPreference(getString(R.string.key_reference)).summary = "Currently: " + settings.currentFiat
        setUserNameSummary()
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.key_prefs_day_night)) {

            App.applyNightMode()
            activity.recreateWhenPossible()
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