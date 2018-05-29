package org.walleth

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import org.walleth.etherscan.EtherScanService

class OnlineInitContentProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        context.startService(Intent(context, EtherScanService::class.java))
        return true
    }

    override fun insert(uri: Uri?, values: ContentValues?) = null
    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?) = null
    override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun getType(uri: Uri?) = null

}