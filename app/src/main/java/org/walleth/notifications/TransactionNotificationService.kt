package org.walleth.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.addresses.AddressBookEntry
import org.walleth.data.transactions.TransactionEntity
import org.walleth.transactions.getTransactionActivityIntentForHash

class TransactionNotificationService : LifecycleService() {

    private val appDatabase: AppDatabase by inject()

    private val allThatWantNotificationsLive by lazy { appDatabase.addressBook.allThatWantNotificationsLive() }
    private var liveDataAllNotifications: LiveData<List<TransactionEntity>>? = null
    private var addressesToNotify: List<Address> = emptyList()
    private val alreadyNotified: MutableSet<String> = mutableSetOf()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val allTransactionsToNotifyObserver = Observer<List<TransactionEntity>> { txList ->
            txList?.let { allTransactionsToNotify ->
                val relevantTransaction = allTransactionsToNotify.firstOrNull {
                    val currentEpochSeconds = LocalDateTime.now().atZone(ZoneOffset.systemDefault()).toEpochSecond()
                    val isRecent = currentEpochSeconds - (it.transaction.creationEpochSecond ?: 0) < 60
                    !it.transactionState.isPending && isRecent && !alreadyNotified.contains(it.hash)
                }

                if (relevantTransaction != null) {
                    alreadyNotified.add(relevantTransaction.hash)
                    val transactionIntent = baseContext.getTransactionActivityIntentForHash(relevantTransaction.hash)
                    val contentIntent = PendingIntent.getActivity(baseContext, 0, transactionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                    val notificationService = baseContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                    if (Build.VERSION.SDK_INT > 25) {
                        val channel = NotificationChannel("transactions", NOTIFICATION_CHANNEL_ID_TRANSACTION_NOTIFICATIONS, NotificationManager.IMPORTANCE_HIGH)
                        channel.description = "View and Stop Geth Service"
                        notificationService.createNotificationChannel(channel)
                    }

                    val notification = NotificationCompat.Builder(baseContext, NOTIFICATION_CHANNEL_ID_TRANSACTION_NOTIFICATIONS).apply {
                        setContentTitle("WallETH Transaction")
                        setContentText("Got transaction")
                        setAutoCancel(true)
                        setContentIntent(contentIntent)
                        val myFrom = relevantTransaction.transaction.from
                        // TODO better handle from==null
                        if (myFrom == null || addressesToNotify.contains(myFrom)) {
                            setSmallIcon(R.drawable.notification_minus)
                        } else {
                            setSmallIcon(R.drawable.notification_plus)
                        }

                    }.build()


                    notificationService.notify(NOTIFICATION_ID_TRANSACTION_NOTIFICATIONS, notification)
                }
            }
        }

        val allThatWantNotificationObserver = Observer<List<AddressBookEntry>> { addressesToNotify ->
            if (addressesToNotify != null) {
                liveDataAllNotifications?.removeObserver(allTransactionsToNotifyObserver)
                liveDataAllNotifications = appDatabase.transactions.getAllTransactionsForAddressLive(addressesToNotify.map { it.address })
                liveDataAllNotifications?.observe(this, allTransactionsToNotifyObserver)
            }

        }
        allThatWantNotificationsLive.removeObserver(allThatWantNotificationObserver)
        allThatWantNotificationsLive.observe(this, allThatWantNotificationObserver)

        return START_STICKY
    }

}
