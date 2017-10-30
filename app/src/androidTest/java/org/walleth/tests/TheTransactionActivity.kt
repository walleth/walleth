package org.walleth.tests

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import org.junit.Rule
import org.junit.Test
import org.kethereum.model.ChainDefinition
import org.kethereum.model.createTransactionWithDefaults
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.activities.ViewTransactionActivity
import org.walleth.activities.getTransactionActivityIntentForHash
import org.walleth.data.ETH_IN_WEI
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.infrastructure.TestApp
import org.walleth.testdata.DEFAULT_TEST_ADDRESS
import org.walleth.testdata.Room77
import org.walleth.testdata.ShapeShift
import org.walleth.testdata.addTestAddresses
import java.math.BigInteger

class TheTransactionActivity {


    @get:Rule
    var rule = TruleskActivityRule(ViewTransactionActivity::class.java, false)

    private val DEFAULT_NONCE = BigInteger("11")
    private val DEFAULT_CHAIN = ChainDefinition(4)
    private val DEFAULT_TX = createTransactionWithDefaults(value = ETH_IN_WEI,
            from = DEFAULT_TEST_ADDRESS,
            to = DEFAULT_TEST_ADDRESS,
            nonce = DEFAULT_NONCE,
            txHash = "0xFOO",
            chain = DEFAULT_CHAIN
    )

    @Test
    fun nonceIsDisplayedCorrectly() {

        TestApp.testDatabase.transactions.upsert(DEFAULT_TX.toEntity(null, TransactionState()))
        TestApp.testDatabase.addressBook.addTestAddresses()
        rule.launchActivity(InstrumentationRegistry.getTargetContext().getTransactionActivityIntentForHash("0xFOO"))

        onView(withId(R.id.nonce)).check(matches(withText("11")))
    }

    @Test
    fun isLabeledToWhenWeReceive() {
        TestApp.testDatabase.addressBook.addTestAddresses()
        val transaction = DEFAULT_TX.copy(from = DEFAULT_TEST_ADDRESS, to = Room77)
        TestApp.testDatabase.transactions.upsert(transaction.toEntity(null, TransactionState()))
        rule.launchActivity(InstrumentationRegistry.getTargetContext().getTransactionActivityIntentForHash(transaction.txHash!!))

        onView(withId(R.id.from_to_title)).check(matches(withText(R.string.transaction_to_label)))
        onView(withId(R.id.from_to)).check(matches(withText("Room77")))
    }


    @Test
    fun isLabeledFromWhenWeReceive() {
        TestApp.testDatabase.addressBook.addTestAddresses()
        val transaction = DEFAULT_TX.copy(from = ShapeShift, to = DEFAULT_TEST_ADDRESS)
        TestApp.testDatabase.transactions.upsert(transaction.toEntity(null, TransactionState()))
        rule.launchActivity(InstrumentationRegistry.getTargetContext().getTransactionActivityIntentForHash(transaction.txHash!!))

        onView(withId(R.id.from_to_title)).check(matches(withText(R.string.transaction_from_label)))
        onView(withId(R.id.from_to)).check(matches(withText("ShapeShift")))
    }


}
