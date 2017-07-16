package org.walleth

import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.walleth.kethereum.android.TransactionParcel
import java.math.BigInteger

class TheTransactionParcel {

    val DEFAULT_NONCE = BigInteger("42")
    @Test
    fun normalTransactionSurvives() {

        val transactionBefore = Transaction(BigInteger("10"), Address("0xab"), Address("0xcd"), nonce = DEFAULT_NONCE)
        testTransactionParcel(transactionBefore)
    }

    @Test
    fun customTransactionSurvives() {

        val transactionBefore = Transaction(BigInteger("1000"), Address("0xab"), Address("0xcd"),
                creationEpochSecond = 10L,
                gasLimit = BigInteger("123"),
                gasPrice = BigInteger("123542"),
                input = listOf(1, 2, 3, 4, 2),
                txHash = "0xfoo")
        testTransactionParcel(transactionBefore)
    }

    @Test
    fun transactionWithNullFieldsSurvives() {

        val transactionBefore = Transaction(BigInteger("0"), Address("0xab"), null)
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
