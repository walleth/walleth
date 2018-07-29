package org.walleth.fcm

import com.google.firebase.iid.FirebaseInstanceIdService
import okhttp3.OkHttpClient
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.walleth.data.AppDatabase
import org.walleth.walletconnect.WalletConnectDriver

class WallethFirebaseInstanceIdService : FirebaseInstanceIdService(), KodeinAware {

    override val kodein by closestKodein()

    override fun onTokenRefresh() {
        val okHttp: OkHttpClient by instance()
        val db: AppDatabase by instance()
        val walletConnectDriver: WalletConnectDriver by instance()

        Thread {
            val addresses = db.addressBook.allThatWantNotifications().map { it.address.hex }
            registerPush(walletConnectDriver, okHttp, addresses)
        }.run()
        super.onTokenRefresh()
    }
}
