package org.walleth.geth

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.CheckBoxPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject
import org.walleth.App
import org.walleth.R
import org.walleth.data.config.Settings
import org.walleth.geth.services.GethLightEthereumService
import org.walleth.geth.services.GethLightEthereumService.Companion.gethStopIntent

class GethInitContentProvider : ContentProvider() {

    class GethInitAppLifecycleObserver(val context: Context, val settings: Settings) : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun connectListener() {

            if (settings.isLightClientWanted()) {
                Intent(context, GethLightEthereumService::class.java).run {
                    ContextCompat.startForegroundService(context, this)
                }
            }

            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        }

    }

    override fun onCreate(): Boolean {

        val settings: Settings by inject()

        App.postInitCallbacks.add {
            context?.let { context ->
                ProcessLifecycleOwner.get().lifecycle.addObserver(GethInitAppLifecycleObserver(context, settings))
            }
        }

        context?.let { context ->
            App.extraPreferences.add(Pair(R.xml.geth_prefs, { prefs ->
                val startLightKey = context?.getString(R.string.key_prefs_start_light)
                val startLightPreference = prefs.findPreference<CheckBoxPreference>(startLightKey)
                startLightPreference?.setOnPreferenceChangeListener { preference, newValue ->

                    if (newValue != GethLightEthereumService.isRunning) {
                        if (GethLightEthereumService.isRunning) {
                            preference.context.startService(context?.gethStopIntent())
                        } else {
                            preference.context.startService(Intent(preference.context, GethLightEthereumService::class.java))
                        }
                        GlobalScope.async(Dispatchers.Main) {
                            val alert = AlertDialog.Builder(preference.context)
                                    .setCancelable(false)
                                    .setMessage(R.string.settings_please_wait)
                                    .show()

                            async(Dispatchers.Default) {
                                while (GethLightEthereumService.isRunning != GethLightEthereumService.shouldRun) {
                                    delay(100)
                                }
                            }.await()
                            alert.dismiss()
                        }
                    }

                    true
                }

            }))
        }
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?) = null
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?) = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun getType(uri: Uri) = null

}