package org.ligi.walleth.data

class FileBackedTransactionProvider() : TransactionProvider {

    val transactionList = mutableListOf<Transaction>()

    override fun getTransactionsForAddress(address: WallethAddress) = mutableListOf<Transaction>()

    override fun addTransaction(transaction: Transaction) {
        transactionList.add(transaction)
    }

    override fun getAllTransactions() = transactionList
}