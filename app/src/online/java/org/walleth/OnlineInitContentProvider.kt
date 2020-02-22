package org.walleth

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.walleth.dataprovider.DataProvidingService

class OnlineInitContentProvider : ContentProvider(), LifecycleObserver {

    override fun onCreate(): Boolean {
        tryStartService()
        return true
    }

    private fun tryStartService() {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        lifecycle.coroutineScope.launch {
            while (!App.isInitialized) {
                delay(500)
            }
            try {
                context?.startService(Intent(context, DataProvidingService::class.java))
                lifecycle.removeObserver(this@OnlineInitContentProvider)
            } catch (ise: IllegalStateException) {
                // happens on android 8+ when app is not in foreground
                lifecycle.addObserver(this@OnlineInitContentProvider)
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onEnterForeground() {
        tryStartService()
    }

    override fun insert(uri: Uri, values: ContentValues?) = null
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?) = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun getType(uri: Uri) = null

}