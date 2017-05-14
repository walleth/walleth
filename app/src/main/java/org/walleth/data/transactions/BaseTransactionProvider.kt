package org.walleth.data.transactions

import org.walleth.data.SimpleObserveable
import org.walleth.data.WallethAddress

open class BaseTransactionProvider : SimpleObserveable(), TransactionProvider {

    override fun getLastNonceForAddress(address: WallethAddress) = getTransactionsForAddress(address).filter { it.from == address }.fold(-1L, { i: Long, transaction: Transaction -> Math.max(i, transaction.nonce ?: -1) })

    val txListLock = Any()

    val transactionList = mutableListOf<Transaction>()

    override fun getTransactionsForHash(hash: String) = synchronized(txListLock) {
        transactionList.firstOrNull { it.txHash?.toUpperCase() == hash.toUpperCase() }
    }

    override fun getTransactionsForAddress(address: WallethAddress) = synchronized(txListLock) {
        transactionList.filter { it.from == address || it.to == address }
    }


    override fun addTransaction(transaction: Transaction) {
        synchronized(txListLock) {
            val firstOrNull = transactionList.firstOrNull { transaction.txHash != null && it.txHash == transaction.txHash }
            if (firstOrNull == null) {
                transactionList.add(transaction)
                promoteChange()
            }
        }
    }

    override fun getAllTransactions() = transactionList
}