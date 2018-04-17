package org.walleth

import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import okhttp3.OkHttpClient
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.fcm.registerPush

class FirebaseIntentService : LifecycleService(), KodeinAware {

    override val kodein by closestKodein()

    override fun onCreate() {
        super.onCreate()

        val appDatabase: AppDatabase by instance()
        val okHttpClient: OkHttpClient by instance()
        appDatabase.addressBook.allThatWantNotificationsLive().observe(this, Observer { list: List<AddressBookEntry>? ->
            list?.let { registerPush(okHttpClient, it.map { it.address.hex }) }
        })
    }

}