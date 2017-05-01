package org.ligi.walleth.data.transactions

import org.ligi.walleth.data.Observeable
import org.ligi.walleth.data.WallethAddress

interface TransactionProvider : Observeable {

    fun getTransactionsForAddress(address: WallethAddress): List<Transaction>
    fun getAllTransactions(): List<Transaction>

    fun addTransaction(transaction: Transaction)
}