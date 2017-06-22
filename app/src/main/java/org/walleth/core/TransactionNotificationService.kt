package org.walleth.core

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.support.v7.app.NotificationCompat
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
import org.walleth.activities.TransactionActivity.Companion.getTransactionActivityIntentForHash
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.transactions.TransactionProvider
import org.walleth.ui.ChangeObserver

class TransactionNotificationService : Service(), KodeinInjected {

    override val injector = KodeinInjector()

    val binder by lazy { Binder() }
    override fun onBind(intent: Intent) = binder

    val lazyKodein = LazyKodein(appKodein)

    val transactionProvider: TransactionProvider by lazyKodein.instance()
    val addressBook: AddressBook by lazyKodein.instance()

    fun Transaction.isNotifyWorthyTransaction(): Boolean {

        if (!(addressBook.isEntryRelevant(from) || addressBook.isEntryRelevant(to!!))) {
            return false
        }

        return LocalDateTime.now().atZone(ZoneOffset.systemDefault()).toEpochSecond() - (creationEpochSecond ?: 0) < 60
    }

    fun AddressBook.isEntryRelevant(address: Address) =
            getEntryForName(address).let { (it != null && it.isNotificationWanted) }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        transactionProvider.registerChangeObserver(object : ChangeObserver {
            override fun observeChange() {
                val transactions = transactionProvider.getAllTransactions()
                        .filter { it.transaction.isNotifyWorthyTransaction() }

                if (!transactions.isEmpty()) {
                    val relevantTransaction = transactions.first()
                    relevantTransaction.transaction.txHash?.let {

                        val transactionIntent = baseContext.getTransactionActivityIntentForHash(it)
                        val contentIntent = PendingIntent.getActivity(baseContext, 0, transactionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                        val notification = NotificationCompat.Builder(baseContext).apply {
                            setContentTitle("WALLETH Transaction")
                            setContentText("Got transaction")
                            setAutoCancel(true)
                            setContentIntent(contentIntent)
                            if (addressBook.isEntryRelevant(relevantTransaction.transaction.from)) {
                                setSmallIcon(R.drawable.notification_minus)
                            } else {
                                setSmallIcon(R.drawable.notification_plus)
                            }

                        }.build()

                        val notificationService = baseContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationService.notify(111, notification)
                    }
                }
            }

        })

        return START_STICKY
    }

}
