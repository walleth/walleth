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
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.walleth.App
import org.walleth.data.config.Settings
import org.walleth.geth.services.GethLightEthereumService
import org.walleth.geth.services.GethTransactionSigner

class GethInitContentProvider : ContentProvider() {

    class GethInitAppLifecycleObserver(val context: Context, val settings: Settings) : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun connectListener() {
            context.startService(Intent(context, GethTransactionSigner::class.java))

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

        return true
    }

    override fun insert(uri: Uri?, values: ContentValues?) = null
    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?) = null
    override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun getType(uri: Uri?) = null

}