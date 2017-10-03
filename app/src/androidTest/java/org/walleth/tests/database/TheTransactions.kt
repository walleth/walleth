package org.walleth.tests.database

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.kethereum.model.ChainDefinition
import org.kethereum.model.createTransactionWithDefaults
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.testdata.Ligi
import org.walleth.testdata.Room77
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
    fun nonceTest() {
        //TODO
    }

}