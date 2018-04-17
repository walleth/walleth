package org.walleth.ui


import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.PreferenceFragmentCompat
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.ligi.kaxt.recreateWhenPossible
import org.walleth.App
import org.walleth.R
import org.walleth.data.config.Settings
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.geth.services.GethLightEthereumService
import org.walleth.geth.services.GethLightEthereumService.Companion.gethStopIntent

class WalletPrefsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener, KodeinAware {

    override val kodein by closestKodein()
    private val settings: Settings by instance()
    private val currentTokenProvider: CurrentTokenProvider by instance()

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        findPreference(getString(R.string.key_reference)).summary = getString(R.string.settings_currently, settings.currentFiat)
        findPreference(getString(R.string.key_token)).summary = getString(R.string.settings_currently,  currentTokenProvider.currentToken.name)

        setUserNameSummary()
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.key_prefs_day_night)) {

            App.applyNightMode(settings)
            activity?.recreateWhenPossible()
        }
        if (key == getString(R.string.key_prefs_start_light)) {
            if ((findPreference(key) as CheckBoxPreference).isChecked != GethLightEthereumService.isRunning) {
                if (GethLightEthereumService.isRunning) {
                    context?.let { it.startService(it.gethStopIntent()) }
                } else {
                    context?.let { it.startService(Intent(context, GethLightEthereumService::class.java)) }
                }
                async(UI) {
                    val alert = AlertDialog.Builder(getContext())
                            .setCancelable(false)
                            .setMessage(R.string.settings_please_wait).show()
                    async(CommonPool) {
                        while (GethLightEthereumService.isRunning != GethLightEthereumService.shouldRun) {
                            delay(100)
                        }
                    }.await()
                    alert.dismiss()
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