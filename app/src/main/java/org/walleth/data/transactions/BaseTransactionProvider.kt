package org.walleth.data.transactions

import org.kethereum.model.Address
import org.walleth.data.SimpleObserveable
import java.math.BigInteger
import java.util.concurrent.ConcurrentLinkedQueue

open class BaseTransactionProvider : SimpleObserveable(), TransactionProvider {

    protected val transactionMap = mutableMapOf<String, TransactionWithState>()
    protected val pendingTransactions = ConcurrentLinkedQueue<TransactionWithState>()

    override fun getPendingTransactions() = pendingTransactions.toList()
    override fun popPendingTransaction() = pendingTransactions.poll()


    override fun addPendingTransaction(transaction: TransactionWithState) {
        pendingTransactions.add(transaction.apply { state.isPending = true })
        promoteChange()
    }

    override fun getLastNonceForAddress(address: Address) = getTransactionsForAddress(address).filter { it.transaction.from == address }.fold(BigInteger("-1"), { i: BigInteger, transaction: TransactionWithState -> i.max(transaction.transaction.nonce ?: BigInteger("-1")) })

    val txListLock = Any()

    override fun clear() {
        transactionMap.clear()
        pendingTransactions.clear()
    }

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
                    val newTransaction = transaction.apply { state.isPending = false }
                    needsUpdate = needsUpdate || (!transactionMap.containsKey(txHash.toUpperCase()) || transactionMap[txHash.toUpperCase()] != newTransaction)
                    transactionMap[txHash.toUpperCase()] = newTransaction
                }
            }
            if (needsUpdate) promoteChange()
        }
    }

    override fun addTransaction(transaction: TransactionWithState) {
        synchronized(txListLock) {
            val txHash = transaction.transaction.txHash
            if (txHash != null) {
                val newTransaction = transaction.apply { state.isPending = false }
                val noUpdate = (transactionMap.containsKey(txHash.toUpperCase()) && transactionMap[txHash.toUpperCase()] == newTransaction)
                transactionMap[txHash.toUpperCase()] = newTransaction
                if (!noUpdate) promoteChange()
            }

        }
    }

    override fun updateTransaction(oldTxHash: String, transaction: TransactionWithState) {
        synchronized(txListLock) {
            if (oldTxHash.toUpperCase() != transaction.transaction.txHash!!.toUpperCase()) {
                transactionMap.remove(oldTxHash.toUpperCase())
            }
            transactionMap[transaction.transaction.txHash!!.toUpperCase()] = transaction.apply { state.isPending = false }
            promoteChange()
        }
    }

    override fun getAllTransactions() = transactionMap.values.toList()
}