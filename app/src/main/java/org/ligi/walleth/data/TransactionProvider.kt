package org.ligi.walleth.data

import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.lazy
import org.greenrobot.eventbus.EventBus
import org.ligi.walleth.App

object TransactionProvider {

    val transactionList = mutableListOf<Transaction>()
    val bus: EventBus by App.kodein.lazy.instance()


    fun getTransactionsForAddress(address: WallethAddress): List<Transaction> = transactionList

    fun addTransaction(transaction: Transaction) {
        transactionList.add(transaction)
        bus.post(TransactionEvent)
    }
}