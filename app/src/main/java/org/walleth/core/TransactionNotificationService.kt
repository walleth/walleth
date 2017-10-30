package org.walleth.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.kethereum.model.Address
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.walleth.R
import org.walleth.activities.getTransactionActivityIntentForHash
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.transactions.TransactionEntity

class TransactionNotificationService : LifecycleService() {

    private val lazyKodein = LazyKodein(appKodein)

    private val appDatabase: AppDatabase by lazyKodein.instance()

    private val allThatWantNotificationsLive by lazy { appDatabase.addressBook.allThatWantNotificationsLive() }
    private var liveDataAllNotifications: LiveData<List<TransactionEntity>>? = null
    private var addressesToNotify: List<Address> = emptyList()
    private val alreadyNotified: MutableSet<String> = mutableSetOf()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val allTransactionsToNotifyObserver = Observer<List<TransactionEntity>> {
            it?.let { allTransactionsToNotify ->
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
                        val channel = NotificationChannel("transactions", "Geth Service", NotificationManager.IMPORTANCE_HIGH)
                        channel.description = "View and Stop Geth Service"
                        notificationService.createNotificationChannel(channel)
                    }

                    val notification = NotificationCompat.Builder(baseContext, "transactions").apply {
                        setContentTitle("WALLETH Transaction")
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


                    notificationService.notify(111, notification)
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
