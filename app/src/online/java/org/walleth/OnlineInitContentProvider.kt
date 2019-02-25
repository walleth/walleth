package org.walleth

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ProcessLifecycleOwner
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import org.walleth.dataprovider.DataProvidingService

class OnlineInitContentProvider : ContentProvider(), LifecycleObserver {

    override fun onCreate(): Boolean {
        tryStartService()
        return true
    }

    private fun tryStartService() {
        try {
            context.startService(Intent(context, DataProvidingService::class.java))
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        } catch (ise: IllegalStateException) {
            // happens on android 8+ when app is not in foreground
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onEnterForeground() {
        tryStartService()
    }

    override fun insert(uri: Uri?, values: ContentValues?) = null
    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?) = null
    override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun getType(uri: Uri?) = null

}