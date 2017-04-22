package org.walleth

import org.ligi.walleth.data.Transaction
import org.ligi.walleth.data.TransactionProvider
import org.ligi.walleth.data.WallethAddress
import org.threeten.bp.LocalDateTime
import java.math.BigInteger

class TransactionProviderWithTestData() : TransactionProvider {

    val transactionList = mutableListOf<Transaction>()

    override fun getTransactionsForAddress(address: WallethAddress): List<Transaction> = mutableListOf(
            Transaction(BigInteger("-420000000000000000"), address, AddressBookWithTestEntries.Room77, localTime = LocalDateTime.now().minusHours(3)),
            Transaction(BigInteger("-500000000000000000"), address, AddressBookWithTestEntries.ΞBay, localTime = LocalDateTime.now().minusHours(5)),
            Transaction(BigInteger("5000000000000000000"), address, AddressBookWithTestEntries.ShapeShift, localTime = LocalDateTime.now().minusHours(23)),
            Transaction(BigInteger("-5000000000000000000"), address, AddressBookWithTestEntries.Faundation, localTime = LocalDateTime.now().minusHours(42)),
            Transaction(BigInteger("1000000000000000000"), address, AddressBookWithTestEntries.Faucet, localTime = LocalDateTime.now().minusHours(64)),
            Transaction(BigInteger("10000000000000000"), address, AddressBookWithTestEntries.Ligi, localTime = LocalDateTime.now().minusHours(96)),
            Transaction(BigInteger("10000000000000000"), address, AddressBookWithTestEntries.Ligi, localTime = LocalDateTime.now().minusHours(128))

    )

    override fun addTransaction(transaction: Transaction) {
        transactionList.add(transaction)
    }

    override fun getAllTransactions() = transactionList
}