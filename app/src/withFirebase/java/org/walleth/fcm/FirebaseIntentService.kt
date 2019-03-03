package org.walleth.fcm

import android.arch.lifecycle.LifecycleService
import org.koin.android.ext.android.inject
import org.ligi.tracedroid.logging.Log
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry

class FirebaseIntentService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        Log.i("starting FirebaseIntentService")
        val appDatabase: AppDatabase by inject()
        appDatabase.addressBook.allThatWantNotificationsLive().observe(this, android.arch.lifecycle.Observer { list: List<AddressBookEntry>? ->

        })
    }

}