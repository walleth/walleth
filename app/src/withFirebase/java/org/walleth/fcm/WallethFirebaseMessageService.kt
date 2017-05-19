package org.walleth.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class WallethFirebaseMessageService : FirebaseMessagingService() {
    override fun onMessageReceived(p0: RemoteMessage?) {
            super.onMessageReceived(p0)
    }
}