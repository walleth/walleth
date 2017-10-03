package org.walleth.core

import android.app.NotificationManager
import android.app.PendingIntent
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import com.github.salomonbrys.kodein.KodeinInjected
import com.github.salomonbrys.kodein.KodeinInjector
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.walleth.R
import org.walleth.activities.ViewTransactionActivity.Companion.getTransactionActivityIntentForHash
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookDAO

class TransactionNotificationService : LifecycleService(), KodeinInjected {

    override val injector = KodeinInjector()

    val lazyKodein = LazyKodein(appKodein)

    val appDatabase: AppDatabase by lazyKodein.instance()
    val addressBook by lazy { appDatabase.addressBook }

    fun Transaction.isNotifyWorthyTransaction(): Boolean {

        val myFrom = from
        if (myFrom == null || !(addressBook.isEntryRelevant(myFrom) || addressBook.isEntryRelevant(to!!))) {
            return false
        }

        return LocalDateTime.now().atZone(ZoneOffset.systemDefault()).toEpochSecond() - (creationEpochSecond ?: 0) < 60
    }


    private fun AddressBookDAO.isEntryRelevant(address: Address) = byAddress(address).let { (it != null && it.isNotificationWanted) }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        appDatabase.transactions.getTransactionsLive().observe(this, Observer {
            val relevantTransactions = it?.filter { it.transaction.isNotifyWorthyTransaction() }

            if (relevantTransactions != null && relevantTransactions.isNotEmpty()) {
                val relevantTransaction = relevantTransactions.first()
                relevantTransaction.transaction.txHash?.let {

                    val transactionIntent = baseContext.getTransactionActivityIntentForHash(it)
                    val contentIntent = PendingIntent.getActivity(baseContext, 0, transactionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                    val notification = NotificationCompat.Builder(baseContext, "transactions").apply {
                        setContentTitle("WALLETH Transaction")
                        setContentText("Got transaction")
                        setAutoCancel(true)
                        setContentIntent(contentIntent)
                        val myFrom = relevantTransaction.transaction.from
                        // TODO better handle from==null
                        if (myFrom == null || addressBook.isEntryRelevant(myFrom)) {
                            setSmallIcon(R.drawable.notification_minus)
                        } else {
                            setSmallIcon(R.drawable.notification_plus)
                        }

                    }.build()

                    val notificationService = baseContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationService.notify(111, notification)
                }
            }
        })

        return START_STICKY
    }

}
