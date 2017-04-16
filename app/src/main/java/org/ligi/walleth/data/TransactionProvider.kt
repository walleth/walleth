package org.ligi.walleth.data

import org.ligi.walleth.App

object TransactionProvider {

    val transactionList = mutableListOf<Transaction>()

    fun getTransactionsForAddress(address: WallethAddress): List<Transaction> = transactionList

    fun addTransaction(transaction: Transaction) {
        transactionList.add(transaction)
        App.bus.post(TransactionEvent)
    }
}