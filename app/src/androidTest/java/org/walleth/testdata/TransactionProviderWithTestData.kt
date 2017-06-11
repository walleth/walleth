package org.walleth.testdata

import org.threeten.bp.LocalDateTime
import org.walleth.data.transactions.BaseTransactionProvider
import org.walleth.data.transactions.Transaction
import java.math.BigInteger

class TransactionProviderWithTestData : BaseTransactionProvider() {

    fun reset() {
        pendingTransactions.clear()
        transactionMap.clear()
    }

    fun load() = mutableListOf(
            Transaction(BigInteger("420000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Room77, localTime = LocalDateTime.now().minusHours(3), txHash = "0x0000001"),
            Transaction(BigInteger("500000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.ΞBay, localTime = LocalDateTime.now().minusHours(5), txHash = "0x0000002"),
            Transaction(BigInteger("5000000000000000000"), AddressBookWithTestEntries.Companion.ShapeShift, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(23), txHash = "0x0000003"),
            Transaction(BigInteger("5000000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Faundation, localTime = LocalDateTime.now().minusHours(42), txHash = "0x0000004"),
            Transaction(BigInteger("1000000000000000000"), AddressBookWithTestEntries.Companion.Faucet, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(64), txHash = "0x0000005"),
            Transaction(BigInteger("10000000000000000"), AddressBookWithTestEntries.Companion.Ligi, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(96), txHash = "0x0000006"),
            Transaction(BigInteger("10000000000000000"), AddressBookWithTestEntries.Companion.Ligi, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(128), txHash = "0x0000007"),
            Transaction(BigInteger("10000000000000000"), AddressBookWithTestEntries.Companion.Faucet, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(150), txHash = "0x0000008"),
            Transaction(BigInteger("420000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Room77, localTime = LocalDateTime.now().minusHours(190), txHash = "0x0000009"),
            Transaction(BigInteger("230000000000000000"), AddressBookWithTestEntries.Companion.ShapeShift, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(210), txHash = "0x0000010"),
            Transaction(BigInteger("500000000000000000"), AddressBookWithTestEntries.Companion.Faundation, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(230), txHash = "0x0000011"),
            Transaction(BigInteger("100000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Ligi, localTime = LocalDateTime.now().minusHours(320), txHash = "0x0000012"),
            Transaction(BigInteger("125000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Room77, localTime = LocalDateTime.now().minusHours(360), txHash = "0x0000013")


    ).forEach { addTransaction(it) }

}