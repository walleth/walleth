package org.walleth.geth

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.support.v4.content.ContextCompat
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.walleth.App
import org.walleth.data.config.Settings
import org.walleth.geth.services.GethLightEthereumService
import org.walleth.geth.services.GethTransactionSigner

class GethInitContentProvider : ContentProvider() {

    private val lazyKodein by lazy { LazyKodein(context.appKodein) }

    override fun onCreate(): Boolean {
        App.postInitCallbacks.add({
            context.startService(Intent(context, GethTransactionSigner::class.java))

            val settings: Settings by lazyKodein.instance()
            if (settings.isLightClientWanted()) {
                Intent(context, GethLightEthereumService::class.java).run {
                    ContextCompat.startForegroundService(context, this)
                }
            }

        })

        return true
    }

    override fun insert(uri: Uri?, values: ContentValues?) = null
    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?) = null
    override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun getType(uri: Uri?) = null

}