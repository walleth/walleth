package org.ligi.walleth.data

interface TransactionProvider {

    fun getTransactionsForAddress(address: WallethAddress): List<Transaction>
    fun getAllTransactions(): List<Transaction>

    fun addTransaction(transaction: Transaction)
}