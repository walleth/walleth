package org.walleth.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.Rule
import org.junit.Test
import org.kethereum.model.ChainId
import org.kethereum.model.createTransactionWithDefaults
import org.komputing.khex.extensions.hexToByteArray
import org.komputing.khex.model.HexString
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.data.ETH_IN_WEI
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.infrastructure.TestApp
import org.walleth.testdata.DEFAULT_TEST_ADDRESS
import org.walleth.testdata.Room77
import org.walleth.testdata.ShapeShift
import org.walleth.testdata.addTestAddresses
import org.walleth.transactions.ViewTransactionActivity
import org.walleth.transactions.getTransactionActivityIntentForHash
import java.math.BigInteger

private val DEFAULT_NONCE = BigInteger("11")
private val DEFAULT_CHAIN = ChainId(4L)
private val DEFAULT_TX = createTransactionWithDefaults(value = ETH_IN_WEI,
        from = DEFAULT_TEST_ADDRESS,
        to = DEFAULT_TEST_ADDRESS,
        nonce = DEFAULT_NONCE,
        txHash = "0xFOO",
        chain = DEFAULT_CHAIN
)

class TheTransactionActivity {


    @get:Rule
    var rule = TruleskActivityRule(ViewTransactionActivity::class.java, false)

    @Test
    fun nonceIsDisplayedCorrectly() {

        TestApp.testDatabase.transactions.upsert(DEFAULT_TX.toEntity(null, TransactionState()))
        TestApp.testDatabase.addressBook.addTestAddresses()
        rule.launchActivity(ApplicationProvider.getApplicationContext<Context>().getTransactionActivityIntentForHash("0xFOO"))

        onView(withId(R.id.nonce)).check(matches(withText("11")))
    }

    @Test
    fun isLabeledToWhenWeReceive() {
        TestApp.testDatabase.addressBook.addTestAddresses()
        val transaction = DEFAULT_TX.copy(from = DEFAULT_TEST_ADDRESS, to = Room77)
        TestApp.testDatabase.transactions.upsert(transaction.toEntity(null, TransactionState()))
        rule.launchActivity(ApplicationProvider.getApplicationContext<Context>().getTransactionActivityIntentForHash(transaction.txHash!!))

        onView(withId(R.id.from_to_title)).check(matches(withText(R.string.transaction_to_label)))
        onView(withId(R.id.from_to)).check(matches(withText("Room77")))
    }


    @Test
    fun isLabeledFromWhenWeReceive() {
        TestApp.testDatabase.addressBook.addTestAddresses()
        val transaction = DEFAULT_TX.copy(from = ShapeShift, to = DEFAULT_TEST_ADDRESS)
        TestApp.testDatabase.transactions.upsert(transaction.toEntity(null, TransactionState()))
        rule.launchActivity(ApplicationProvider.getApplicationContext<Context>().getTransactionActivityIntentForHash(transaction.txHash!!))

        onView(withId(R.id.from_to_title)).check(matches(withText(R.string.transaction_from_label)))
        onView(withId(R.id.from_to)).check(matches(withText("ShapeShift")))
    }


    @Test
    fun showsTheCorrectMethodSignature() {
        val transaction = DEFAULT_TX.copy(from = ShapeShift, to = DEFAULT_TEST_ADDRESS,
                input = HexString("0xdeafbeef000000000000000000000000f44f28b5ca7808b9ad782c759ab8efb041de64d2").hexToByteArray())

        TestApp.testDatabase.runInTransaction {
            TestApp.testDatabase.addressBook.addTestAddresses()
            TestApp.testDatabase.transactions.upsert(transaction.toEntity(null, TransactionState()))
        }

        rule.launchActivity(ApplicationProvider.getApplicationContext<Context>().getTransactionActivityIntentForHash(transaction.txHash!!))

        onView(withId(R.id.function_call)).check(matches(withText(
                allOf(containsString(TestApp.contractFunctionTextSignature1), containsString(TestApp.contractFunctionTextSignature2)))))
    }


}
