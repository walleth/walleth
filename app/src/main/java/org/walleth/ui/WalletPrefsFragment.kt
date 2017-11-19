package org.walleth.ui


import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.PreferenceFragmentCompat
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.ligi.kaxt.recreateWhenPossible
import org.walleth.App
import org.walleth.R
import org.walleth.core.GethLightEthereumService
import org.walleth.core.GethLightEthereumService.Companion.gethStopIntent
import org.walleth.data.config.Settings
import org.walleth.data.tokens.CurrentTokenProvider

class WalletPrefsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val settings: Settings by LazyKodein(appKodein).instance()
    private val currentTokenProvider: CurrentTokenProvider by LazyKodein(appKodein).instance()

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
            activity.recreateWhenPossible()
        }
        if (key == getString(R.string.key_prefs_start_light)) {
            if ((findPreference(key) as CheckBoxPreference).isChecked != GethLightEthereumService.isRunning) {
                if (GethLightEthereumService.isRunning) {
                    context.startService(context.gethStopIntent())
                } else {
                    context.startService(Intent(context, GethLightEthereumService::class.java))
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