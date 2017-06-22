package org.walleth.data.transactions

import org.kethereum.model.Address
import org.walleth.data.SimpleObserveable
import java.util.concurrent.ConcurrentLinkedQueue

open class BaseTransactionProvider : SimpleObserveable(), TransactionProvider {


    protected val pendingTransactions = ConcurrentLinkedQueue<TransactionWithState>()

    override fun getPendingTransactions() = pendingTransactions.toList()


    override fun popPendingTransaction() = pendingTransactions.poll()


    override fun addPendingTransaction(transaction: TransactionWithState) {
        pendingTransactions.add(transaction)
        promoteChange()
    }

    override fun getLastNonceForAddress(address: Address) = getTransactionsForAddress(address).filter { it.transaction.from == address }.fold(-1L, { i: Long, transaction: TransactionWithState -> Math.max(i, transaction.transaction.nonce ?: -1) })

    val txListLock = Any()

    protected val transactionMap = mutableMapOf<String, TransactionWithState>()

    override fun getTransactionForHash(hash: String) = synchronized(txListLock) {
        transactionMap[hash.toUpperCase()]
    }

    override fun getTransactionsForAddress(address: Address) = synchronized(txListLock) {
        transactionMap.values.filter { it.transaction.from == address || it.transaction.to == address }
                .plus(pendingTransactions.filter { it.transaction.from == address || it.transaction.to == address })

    }


    override fun addTransactions(transactions: List<TransactionWithState>) {
        synchronized(txListLock) {
            var needsUpdate = false
            for (transaction in transactions) {
                val txHash = transaction.transaction.txHash
                if (txHash != null) {
                    needsUpdate = needsUpdate || (!transactionMap.containsKey(txHash.toUpperCase()) || transactionMap[txHash.toUpperCase()] != transaction)
                    transactionMap[txHash.toUpperCase()] = transaction
                }
            }
            if (needsUpdate) promoteChange()
        }
    }
    override fun addTransaction(transaction: TransactionWithState) {
        synchronized(txListLock) {
            val txHash = transaction.transaction.txHash
            if (txHash != null) {
                val noUpdate = (transactionMap.containsKey(txHash.toUpperCase()) && transactionMap[txHash.toUpperCase()] == transaction)
                transactionMap[txHash.toUpperCase()] = transaction
                if (!noUpdate) promoteChange()
            }

        }
    }

    override fun updateTransaction(oldTxHash: String, transaction: TransactionWithState) {
        synchronized(txListLock) {
            if (oldTxHash.toUpperCase() != transaction.transaction.txHash!!.toUpperCase()) {
                transactionMap.remove(oldTxHash.toUpperCase())
            }
            transactionMap[transaction.transaction.txHash!!.toUpperCase()] = transaction
            promoteChange()
        }
    }

    override fun getAllTransactions() = transactionMap.values.toList()
}