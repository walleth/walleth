package org.walleth

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.walleth.data.addressbook.AddressBook
import org.walleth.fcm.registerPush
import org.walleth.ui.ChangeObserver

class FirebaseInitContentProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val addressBook: AddressBook = context.appKodein.invoke().instance()
        addressBook.registerChangeObserver(object : ChangeObserver {
            override fun observeChange() {
                registerPush(context.appKodein.invoke())
            }
        })
        return true
    }

    override fun insert(uri: Uri?, values: ContentValues?) = null
    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?) = null
    override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) =0
    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?) =0
    override fun getType(uri: Uri?) = null

}