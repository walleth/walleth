package org.walleth.ui


import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import org.koin.android.ext.android.inject
import org.ligi.kaxt.recreateWhenPossible
import org.walleth.App
import org.walleth.R
import org.walleth.data.config.Settings
import org.walleth.data.tokens.CurrentTokenProvider

class WalletPrefsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val settings: Settings by inject()
    private val currentTokenProvider: CurrentTokenProvider by inject()

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        findPreference(getString(R.string.key_reference)).summary = getString(R.string.settings_currently, settings.currentFiat)
        findPreference(getString(R.string.key_token)).summary = getString(R.string.settings_currently, currentTokenProvider.currentToken.name)

        setUserNameSummary()
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        try {
            if (key == getString(R.string.key_prefs_day_night)) {

                App.applyNightMode(settings)
                activity?.recreateWhenPossible()
            }
            if (key == getString(R.string.key_noscreenshots)) {
                activity?.recreateWhenPossible()
            }
            if (key == getString(R.string.key_prefs_start_light)) {

            }
            setUserNameSummary()
        } catch (ignored: IllegalStateException) {
        }
    }

    private fun setUserNameSummary() {
        findPreference(getString(R.string.key_prefs_stats_username)).summary = settings.getStatsName() + " @ https://stats.rinkeby.io"
    }

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        App.extraPreferences.forEach {
            addPreferencesFromResource(it.first)
            it.second(preferenceScreen)
        }
    }

}