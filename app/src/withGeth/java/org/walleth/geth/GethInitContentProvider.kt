package org.walleth.geth

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ProcessLifecycleOwner
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.Preference
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.walleth.App
import org.walleth.R
import org.walleth.activities.OfflineTransactionActivity
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
        val kodein by closestKodein(context)
        val settings: Settings by kodein.instance()

        App.postInitCallbacks.add({
            ProcessLifecycleOwner.get().lifecycle.addObserver(GethInitAppLifecycleObserver(context, settings))
        })

        val preference = Preference(context)
        val enablePreference = CheckBoxPreference(context)

        preference.setOnPreferenceChangeListener { _, newValue ->

            if (newValue != GethLightEthereumService.isRunning) {
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

            true
        }
        preference.title = "foo_yolo"
        preference.intent = Intent(context, OfflineTransactionActivity::class.java)
        App.extraPreferences.add(preference)

        return true
    }

    override fun insert(uri: Uri?, values: ContentValues?) = null
    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?) = null
    override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun getType(uri: Uri?) = null

}