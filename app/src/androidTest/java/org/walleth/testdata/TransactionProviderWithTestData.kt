package org.walleth.testdata

import org.kethereum.model.Transaction
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.walleth.data.transactions.BaseTransactionProvider
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.TransactionWithState
import java.math.BigInteger

class TransactionProviderWithTestData : BaseTransactionProvider() {

    fun reset() {
        pendingTransactions.clear()
        transactionMap.clear()
    }

    fun load() = mutableListOf(
            Transaction(BigInteger("420000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Room77, creationEpochSecond = LocalDateTime.now().minusHours(3).toEpochSecond(), txHash = "0x0000001"),
            Transaction(BigInteger("500000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.ΞBay, creationEpochSecond = LocalDateTime.now().minusHours(5).toEpochSecond(), txHash = "0x0000002"),
            Transaction(BigInteger("5000000000000000000"), AddressBookWithTestEntries.Companion.ShapeShift, DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(23).toEpochSecond(), txHash = "0x0000003"),
            Transaction(BigInteger("5000000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Faundation, creationEpochSecond = LocalDateTime.now().minusHours(42).toEpochSecond(), txHash = "0x0000004"),
            Transaction(BigInteger("1000000000000000000"), AddressBookWithTestEntries.Companion.Faucet, DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(64).toEpochSecond(), txHash = "0x0000005"),
            Transaction(BigInteger("10000000000000000"), AddressBookWithTestEntries.Companion.Ligi, DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(96).toEpochSecond(), txHash = "0x0000006"),
            Transaction(BigInteger("10000000000000000"), AddressBookWithTestEntries.Companion.Ligi, DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(128).toEpochSecond(), txHash = "0x0000007"),
            Transaction(BigInteger("10000000000000000"), AddressBookWithTestEntries.Companion.Faucet, DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(150).toEpochSecond(), txHash = "0x0000008"),
            Transaction(BigInteger("420000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Room77, creationEpochSecond = LocalDateTime.now().minusHours(190).toEpochSecond(), txHash = "0x0000009"),
            Transaction(BigInteger("230000000000000000"), AddressBookWithTestEntries.Companion.ShapeShift, DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(210).toEpochSecond(), txHash = "0x0000010"),
            Transaction(BigInteger("500000000000000000"), AddressBookWithTestEntries.Companion.Faundation, DEFAULT_TEST_ADDRESS, creationEpochSecond = LocalDateTime.now().minusHours(230).toEpochSecond(), txHash = "0x0000011"),
            Transaction(BigInteger("100000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Ligi, creationEpochSecond = LocalDateTime.now().minusHours(320).toEpochSecond(), txHash = "0x0000012"),
            Transaction(BigInteger("125000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Room77, creationEpochSecond = LocalDateTime.now().minusHours(360).toEpochSecond(), txHash = "0x0000013")


    ).forEach { addTransaction(TransactionWithState(it, TransactionState())) }

}

private fun LocalDateTime.toEpochSecond() = atZone(ZoneId.systemDefault()).toEpochSecond()
