package org.walleth.fcm

import com.github.salomonbrys.kodein.android.appKodein
import com.google.firebase.iid.FirebaseInstanceIdService

class WallethFirebaseInstanceIdService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        registerPush(baseContext.appKodein.invoke())
        super.onTokenRefresh()
    }
}
