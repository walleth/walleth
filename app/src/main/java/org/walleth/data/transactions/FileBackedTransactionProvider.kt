package org.walleth.data.transactions

import org.walleth.data.SimpleObserveable
import org.walleth.data.WallethAddress

class FileBackedTransactionProvider : SimpleObserveable(), TransactionProvider {

    val lock = Any()

    val transactionList = mutableListOf<Transaction>()

    override fun getTransactionsForHash(hash: String) = synchronized(lock) {
        transactionList.firstOrNull { it.txHash?.toUpperCase() == hash.toUpperCase() }
    }

    override fun getTransactionsForAddress(address: WallethAddress) = synchronized(lock) {
        transactionList.filter { it.from == address || it.to == address }
    }


    override fun addTransaction(transaction: Transaction) {
        synchronized(lock) {
            val firstOrNull = transactionList.firstOrNull { transaction.txHash != null && it.txHash == transaction.txHash }
            if (firstOrNull == null) {
                transactionList.add(transaction)
                promoteChange()
            }
        }
    }

    override fun getAllTransactions() = transactionList
}