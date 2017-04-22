package org.ligi.walleth.data

import org.greenrobot.eventbus.EventBus

class FileBackedTransactionProvider(val bus: EventBus) : TransactionProvider {

    val transactionList = mutableListOf<Transaction>()

    override fun getTransactionsForAddress(address: WallethAddress) = mutableListOf<Transaction>()

    override fun addTransaction(transaction: Transaction) {
        transactionList.add(transaction)
        bus.post(TransactionEvent)
    }

    override fun getAllTransactions() = transactionList
}