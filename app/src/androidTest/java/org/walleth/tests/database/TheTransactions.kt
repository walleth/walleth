package org.walleth.tests.database

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.kethereum.model.ChainDefinition
import org.kethereum.model.Transaction
import org.kethereum.model.createTransactionWithDefaults
import org.walleth.data.transactions.FunctionCall
import org.walleth.data.transactions.TransactionEntity
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.testdata.*
import java.math.BigInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TheTransactions : AbstractDatabaseTest() {

    @Test
    fun weCanInsertOne() {
        val tx = createTransactionWithDefaults(from = Ligi, to = Room77, value = BigInteger.ONE, txHash = "0xf00", chain = ChainDefinition(42))
        val transactionEntity = tx.toEntity(null, TransactionState())
        database.transactions.upsert(transactionEntity)

        assertThat(database.transactions.getTransactions()).containsExactly(transactionEntity)
    }

    @Test
    fun weCanFindTransactionsForListsOfAddresses() {
        val tx = createTransactionWithDefaults(from = Ligi, to = Room77, value = BigInteger.ONE, txHash = "0xf00", chain = ChainDefinition(42))
        val tx2 = createTransactionWithDefaults(from = Ligi, to = ShapeShift, value = BigInteger.ONE, txHash = "0xf00", chain = ChainDefinition(42))
        val tx3 = createTransactionWithDefaults(from = Faucet, to = ShapeShift, value = BigInteger.ONE, txHash = "0xf00", chain = ChainDefinition(42))
        val tx4 = createTransactionWithDefaults(from = Faundation, to = Ligi, value = BigInteger.ONE, txHash = "0xf00", chain = ChainDefinition(42))


        addTransactions(listOf(tx, tx2, tx3, tx4))

        assertThat(database.transactions.getAllTransactionsForAddress(listOf(Ligi, Room77)).size).isEqualTo(3)
    }

    @Test
    fun weCanFindTransactionsByRelevantAddress() {
        val chain = ChainDefinition(24)
        val tx = createTransactionWithDefaults(from = Ligi, to = ΞBay, value = BigInteger.ZERO, txHash = "0x1", chain = chain)

        database.transactions.upsert(tx.toEntity(null, TransactionState(), FunctionCall(Room77)))

        assertThat(getValue(database.transactions.getIncomingTransactionsForAddressOnChainOrdered(Room77, chain))).hasSize(1)
        assertThat(getValue(database.transactions.getOutgoingTransactionsForAddressOnChainOrdered(Room77, chain))).hasSize(0)
    }

    private fun addTransactions(tx: List<Transaction>) {
        database.transactions.upsert(tx.mapIndexed { index, transaction -> transaction.copy(txHash = "0x" + index).toEntity(null, TransactionState()) })

    }

    @Test
    fun nonceTest() {
        //TODO
    }


}

inline fun <reified T> getValue(liveData: LiveData<T>): T? {
    val data = Array<T?>(1, { i -> null })
    val latch = CountDownLatch(1);
    val observer = object : Observer<T> {
        override fun onChanged(t: T?) {
            data[0] = t
            latch.countDown();
            liveData.removeObserver(this);
        }
    }
    liveData.observeForever(observer);
    latch.await(2, TimeUnit.SECONDS);
    //noinspection unchecked
    return data[0]
}