package org.walleth

import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.fcm.registerPush

class FirebaseIntentService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()

        val appDatabase: AppDatabase = appKodein.invoke().instance()
        appDatabase.addressBook.allThatWantNotificationsLive().observe(this, Observer { list: List<AddressBookEntry>? ->
            list?.let { registerPush(appKodein.invoke().instance(), it.map { it.address.hex }) }
        })
    }

}