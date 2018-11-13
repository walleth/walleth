package org.walleth.fcm

import com.google.firebase.iid.FirebaseInstanceIdService
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import org.walleth.data.AppDatabase
import org.walleth.walletconnect.WalletConnectDriver

class WallethFirebaseInstanceIdService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        val okHttp: OkHttpClient by inject()
        val db: AppDatabase by inject()
        val walletConnectDriver: WalletConnectDriver by inject()

        Thread {
            val addresses = db.addressBook.allThatWantNotifications().map { it.address.hex }
            registerPush(walletConnectDriver, okHttp, addresses)
        }.run()
        super.onTokenRefresh()
    }
}
