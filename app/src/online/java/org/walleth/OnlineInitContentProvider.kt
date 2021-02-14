package org.walleth

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import org.walleth.dataprovider.DataProvidingService

class OnlineInitContentProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val observer = {
            tryStartService()
        }
        App.onActivityToForegroundObserver.add(observer)

        return true
    }

    private fun tryStartService() {
        ContextCompat.startForegroundService(context!!, Intent(context, DataProvidingService::class.java))
    }

    override fun insert(uri: Uri, values: ContentValues?) = null
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?) = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun getType(uri: Uri) = null

}