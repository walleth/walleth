package org.walleth.tests

import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.kethereum.model.Address
import org.kethereum.model.ChainId
import org.kethereum.model.Transaction
import org.kethereum.model.createTransactionWithDefaults
import org.walleth.kethereum.android.TransactionParcel
import java.math.BigInteger

private val DEFAULT_NONCE = BigInteger("42")

class TheTransactionParcel {

    private fun createTx() = createTransactionWithDefaults(
            from = Address("0xab"),
            to = Address("0xcd"),
            value = BigInteger("10"),
            chain = ChainId(4L)
    )

    @Test
    fun normalTransactionSurvives() {

        val transactionBefore = createTx().copy(to = Address("0xcd"), nonce = DEFAULT_NONCE)
        testTransactionParcel(transactionBefore)
    }

    @Test
    fun customTransactionSurvives() {

        val transactionBefore = createTx().copy(
                creationEpochSecond = 10L,
                gasLimit = BigInteger("123"),
                gasPrice = BigInteger("123542"),
                input = ByteArray(0) { (it + 1).toByte() },
                txHash = "0xfoo")
        testTransactionParcel(transactionBefore)
    }

    @Test
    fun transactionWithNullFieldsSurvives() {

        val transactionBefore = createTx().copy(to = null)
        testTransactionParcel(transactionBefore)
    }

    private fun testTransactionParcel(transactionBefore: Transaction) {
        val transactionParcel = TransactionParcel(transactionBefore)

        val obtain = Parcel.obtain()
        transactionParcel.writeToParcel(obtain, 0)

        obtain.setDataPosition(0)

        val transactionAfter = TransactionParcel.createFromParcel(obtain).transaction

        assertThat(transactionAfter).isEqualTo(transactionBefore)
    }
}
