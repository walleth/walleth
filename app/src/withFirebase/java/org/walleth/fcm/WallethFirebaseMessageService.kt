package org.walleth.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.getActivity
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject
import org.ligi.kaxt.getNotificationManager
import org.walleth.R
import org.walleth.walletconnect.WalletConnectDriver
import org.walleth.walletconnect.createIntentForTransaction

class WallethFirebaseMessageService : FirebaseMessagingService() {

    private val walletConnectInteractor: WalletConnectDriver by inject()

    override fun onMessageReceived(p0: RemoteMessage?) {

        super.onMessageReceived(p0)

        p0?.data?.let { data ->

            data["sessionId"]?.let { sessionId ->

                val calls = walletConnectInteractor.getCalls(sessionId)

                if (walletConnectInteractor.txAction != null) {
                    walletConnectInteractor.txAction?.invoke(calls!!)
                } else {
                    val pendingIntent = getActivity(
                            this,
                            0,
                            createIntentForTransaction(calls!!),
                            FLAG_CANCEL_CURRENT
                    )
                    if (Build.VERSION.SDK_INT > 25) {
                        val channel = NotificationChannel("walletconnect", "Wallet Connect", NotificationManager.IMPORTANCE_HIGH)
                        channel.description = "WalletConnect Notification"
                        getNotificationManager().createNotificationChannel(channel)
                    }

                    val notification = NotificationCompat.Builder(this, "walletconnect")
                            .setContentTitle("WalletConnect Request")
                            .setContentText("Interaction with " + calls.session.dappName)
                            .setContentIntent(pendingIntent)
                            .setSmallIcon(R.drawable.ic_notification_walletconnect)
                            .setAutoCancel(true)
                            .build()
                    getNotificationManager().notify(100, notification)
                }

            }
        }
    }
}