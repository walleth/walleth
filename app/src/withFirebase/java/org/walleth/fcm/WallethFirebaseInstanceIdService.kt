package org.walleth.fcm

import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.google.firebase.iid.FirebaseInstanceIdService
import okhttp3.OkHttpClient
import org.walleth.data.AppDatabase

class WallethFirebaseInstanceIdService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        val okHttp: OkHttpClient = baseContext.appKodein.invoke().instance()
        val db: AppDatabase = baseContext.appKodein.invoke().instance()
        Thread {
            val addresses = db.addressBook.allThatWantNotifications().map { it.address.hex }
            registerPush(okHttp, addresses)
        }.run()
        super.onTokenRefresh()
    }
}
