package org.walleth.tests.database

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.kethereum.model.ChainDefinition
import org.kethereum.model.Transaction
import org.kethereum.model.createTransactionWithDefaults
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.testdata.*
import java.math.BigInteger

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

    private fun addTransactions(tx: List<Transaction>) {
        database.transactions.upsert(tx.mapIndexed { index, transaction -> transaction.copy(txHash = "0x"+index).toEntity(null, TransactionState()) })

    }

    @Test
    fun nonceTest() {
        //TODO
    }

}