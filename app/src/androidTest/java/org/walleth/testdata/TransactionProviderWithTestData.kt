package org.walleth.testdata

import org.threeten.bp.LocalDateTime
import org.walleth.data.transactions.BaseTransactionProvider
import org.walleth.data.transactions.Transaction
import java.math.BigInteger

class TransactionProviderWithTestData : BaseTransactionProvider() {

    val mutableListOf =  mutableListOf<Transaction>()

    fun load() = mutableListOf(
                Transaction(BigInteger("420000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Room77, localTime = LocalDateTime.now().minusHours(3)),
                Transaction(BigInteger("500000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.ΞBay, localTime = LocalDateTime.now().minusHours(5)),
                Transaction(BigInteger("5000000000000000000"), AddressBookWithTestEntries.Companion.ShapeShift, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(23)),
                Transaction(BigInteger("5000000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Faundation, localTime = LocalDateTime.now().minusHours(42)),
                Transaction(BigInteger("1000000000000000000"), AddressBookWithTestEntries.Companion.Faucet, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(64)),
                Transaction(BigInteger("10000000000000000"), AddressBookWithTestEntries.Companion.Ligi, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(96)),
                Transaction(BigInteger("10000000000000000"), AddressBookWithTestEntries.Companion.Ligi, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(128)),

                Transaction(BigInteger("10000000000000000"), AddressBookWithTestEntries.Companion.Faucet, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(150)),
                Transaction(BigInteger("420000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Room77, localTime = LocalDateTime.now().minusHours(190)),
                Transaction(BigInteger("230000000000000000"), AddressBookWithTestEntries.Companion.ShapeShift, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(210)),
                Transaction(BigInteger("500000000000000000"), AddressBookWithTestEntries.Companion.Faundation, DEFAULT_TEST_ADDRESS, localTime = LocalDateTime.now().minusHours(230)),
                Transaction(BigInteger("100000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Ligi, localTime = LocalDateTime.now().minusHours(320)),
                Transaction(BigInteger("125000000000000000"), DEFAULT_TEST_ADDRESS, AddressBookWithTestEntries.Companion.Room77, localTime = LocalDateTime.now().minusHours(360))

        ).forEach { addTransaction(it) }


    override val transactionList = mutableListOf
}