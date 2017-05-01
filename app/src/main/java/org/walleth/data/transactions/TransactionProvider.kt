package org.walleth.data.transactions

import org.walleth.data.Observeable
import org.walleth.data.WallethAddress

interface TransactionProvider : Observeable {

    fun getTransactionsForAddress(address: WallethAddress): List<Transaction>
    fun getTransactionsForHash(hash: String): Transaction?

    fun getAllTransactions(): List<Transaction>

    fun addTransaction(transaction: Transaction)
}