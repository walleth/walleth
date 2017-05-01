package org.ligi.walleth.data.transactions

import org.ligi.walleth.data.SimpleObserveable
import org.ligi.walleth.data.WallethAddress

class FileBackedTransactionProvider : SimpleObserveable(), TransactionProvider {

    override fun getTransactionsForHash(hash: String) = transactionList.firstOrNull { it.txHash?.toUpperCase() == hash.toUpperCase() }

    val transactionList = mutableListOf<Transaction>()

    override fun getTransactionsForAddress(address: WallethAddress) = transactionList.filter { it.from == address || it.to == address }

    override fun addTransaction(transaction: Transaction) {
        val firstOrNull = transactionList.firstOrNull { transaction.txHash != null && it.txHash == transaction.txHash }
        if (firstOrNull == null) {
            transactionList.add(transaction)
            promoteChange()
        }
    }

    override fun getAllTransactions() = transactionList
}