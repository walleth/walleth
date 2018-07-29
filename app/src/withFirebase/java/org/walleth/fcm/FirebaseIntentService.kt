package org.walleth.fcm

import android.arch.lifecycle.LifecycleService
import okhttp3.OkHttpClient
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.ligi.tracedroid.logging.Log
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.walletconnect.WalletConnectDriver

class FirebaseIntentService : LifecycleService(), KodeinAware {

    override val kodein by closestKodein()

    override fun onCreate() {
        super.onCreate()
        Log.i("starting FirebaseIntentService")
        val appDatabase: AppDatabase by instance()
        val okHttpClient: OkHttpClient by instance()
        val walletConnectInteractor: WalletConnectDriver by instance()
        appDatabase.addressBook.allThatWantNotificationsLive().observe(this, android.arch.lifecycle.Observer { list: List<AddressBookEntry>? ->
            list?.let { registerPush(walletConnectInteractor, okHttpClient, it.map { it.address.hex }) }
        })
    }

}