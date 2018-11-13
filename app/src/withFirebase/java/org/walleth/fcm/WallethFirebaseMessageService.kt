package org.walleth.fcm

import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.getActivity
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject
import org.ligi.kaxt.getNotificationManager
import org.ligi.tracedroid.logging.Log
import org.walleth.R
import org.walleth.walletconnect.WalletConnectDriver
import org.walleth.walletconnect.createIntentForTransaction

class WallethFirebaseMessageService : FirebaseMessagingService() {

    private val walletConnectInteractor: WalletConnectDriver by inject()

    override fun onMessageReceived(p0: RemoteMessage?) {

        super.onMessageReceived(p0)

        p0?.data?.let { data ->
            Log.i("Received Firebase message $data")

            data["transactionId"]?.let { transactionId ->
                data["sessionId"]?.let { sessionId ->
                    Log.i("Got Wallet Connect mesage with transactionId:$transactionId and sessionId: $sessionId")

                    val tx = walletConnectInteractor.getTransaction(transactionId, sessionId)

                    if (walletConnectInteractor.txAction != null) {
                        walletConnectInteractor.txAction?.invoke(tx!!)
                    } else {
                        val pendingIntent = getActivity(
                                this,
                                0,
                                createIntentForTransaction(tx!!),
                                FLAG_CANCEL_CURRENT
                        )
                        val notification = NotificationCompat.Builder(this, "tx")
                                .setContentTitle(getString(R.string.geth_service_notification_title))
                                .setContentText("Interaction with " + tx.session.dappName)
                                .setContentIntent(pendingIntent)
                                .setSmallIcon(R.drawable.notification)
                                .setAutoCancel(true)
                                .build()
                        getNotificationManager().notify(100, notification)
                    }

                }
            }
        }
    }
}