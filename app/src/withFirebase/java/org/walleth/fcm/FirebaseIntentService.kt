package org.walleth.fcm

import androidx.lifecycle.LifecycleService
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import org.ligi.tracedroid.logging.Log
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.walletconnect.WalletConnectDriver

class FirebaseIntentService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        Log.i("starting FirebaseIntentService")
        val appDatabase: AppDatabase by inject()
        val okHttpClient: OkHttpClient by inject()
        val walletConnectInteractor: WalletConnectDriver by inject()
        appDatabase.addressBook.allThatWantNotificationsLive().observe(this, androidx.lifecycle.Observer { list: List<AddressBookEntry>? ->
            list?.let { registerPush(walletConnectInteractor, okHttpClient, it.map { it.address.hex }) }
        })
    }

}