package org.walleth.data.transactions

import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.walleth.data.Observeable
import org.walleth.data.transactions.TransactionSource.WALLETH

data class TransactionState(var needsSigningConfirmation: Boolean = false,
                            var ref: TransactionSource = WALLETH,
                            var eventLog: String? = null,
                            var error: String? = null)

data class TransactionWithState(val transaction: Transaction, val state: TransactionState)

interface TransactionProvider : Observeable {

    fun getTransactionsForAddress(address: Address): List<TransactionWithState>
    fun getLastNonceForAddress(address: Address): Long

    fun getTransactionForHash(hash: String): TransactionWithState?

    fun getAllTransactions(): List<TransactionWithState>

    fun addTransaction(transaction: TransactionWithState)
    fun addTransactions(transactions: List<TransactionWithState>)

    fun getPendingTransactions(): List<TransactionWithState>
    fun addPendingTransaction(transaction: TransactionWithState)

    fun popPendingTransaction(): TransactionWithState?

    fun updateTransaction(oldTxHash: String, transaction: TransactionWithState)

}