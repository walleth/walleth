package org.walleth.data.transactions

import org.walleth.data.SimpleObserveable
import org.walleth.data.WallethAddress
import java.util.concurrent.ConcurrentLinkedQueue

open class BaseTransactionProvider : SimpleObserveable(), TransactionProvider {


    protected val pendingTransactions = ConcurrentLinkedQueue<Transaction>()

    override fun getPendingTransactions() = pendingTransactions.toList()


    override fun popPendingTransaction() = pendingTransactions.poll()


    override fun addPendingTransaction(transaction: Transaction) {
        pendingTransactions.add(transaction)
        promoteChange()
    }

    override fun getLastNonceForAddress(address: WallethAddress) = getTransactionsForAddress(address).filter { it.from == address }.fold(-1L, { i: Long, transaction: Transaction -> Math.max(i, transaction.nonce ?: -1) })

    val txListLock = Any()

    protected val transactionMap = mutableMapOf<String, Transaction>()

    override fun getTransactionForHash(hash: String) = synchronized(txListLock) {
        transactionMap[hash.toUpperCase()]
    }

    override fun getTransactionsForAddress(address: WallethAddress) = synchronized(txListLock) {
        transactionMap.values.filter { it.from == address || it.to == address }
                .plus(pendingTransactions.filter { it.from == address || it.to == address })

    }


    override fun addTransactions(transactions: List<Transaction>) {
        synchronized(txListLock) {
            var needsUpdate = false
            for (transaction in transactions) {
                val txHash = transaction.txHash
                if (txHash != null) {
                    needsUpdate = needsUpdate || (!transactionMap.containsKey(txHash.toUpperCase()) || transactionMap[txHash.toUpperCase()] != transaction)
                    transactionMap[txHash.toUpperCase()] = transaction
                }
            }
            if (needsUpdate) promoteChange()
        }
    }
    override fun addTransaction(transaction: Transaction) {
        synchronized(txListLock) {
            val txHash = transaction.txHash
            if (txHash != null) {
                val noUpdate = (transactionMap.containsKey(txHash.toUpperCase()) && transactionMap[txHash.toUpperCase()] == transaction)
                transactionMap[txHash.toUpperCase()] = transaction
                if (!noUpdate) promoteChange()
            }

        }
    }

    override fun updateTransaction(oldTxHash: String, transaction: Transaction) {
        synchronized(txListLock) {
            if (oldTxHash.toUpperCase() != transaction.txHash!!.toUpperCase()) {
                transactionMap.remove(oldTxHash.toUpperCase())
            }
            transactionMap[transaction.txHash!!.toUpperCase()] = transaction
            promoteChange()
        }
    }

    override fun getAllTransactions() = transactionMap.values.toList()
}