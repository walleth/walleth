package org.walleth

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import org.junit.Rule
import org.junit.Test
import org.kethereum.model.Transaction
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.activities.TransactionActivity
import org.walleth.activities.TransactionActivity.Companion.getTransactionActivityIntentForHash
import org.walleth.data.ETH_IN_WEI
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.TransactionWithState
import org.walleth.infrastructure.TestApp
import org.walleth.testdata.AddressBookWithTestEntries.Companion.Room77
import org.walleth.testdata.AddressBookWithTestEntries.Companion.ShapeShift
import org.walleth.testdata.DEFAULT_TEST_ADDRESS

class TheTransactionActivity {

    @get:Rule
    var rule = TruleskActivityRule(TransactionActivity::class.java, false)

    @Test
    fun nonceIsDisplayedCorrectly() {
        TestApp.transactionProvider.addTransaction(TransactionWithState(Transaction(ETH_IN_WEI, DEFAULT_TEST_ADDRESS, DEFAULT_TEST_ADDRESS, nonce = 11, txHash = "0xFOO"), TransactionState()))

        rule.launchActivity(InstrumentationRegistry.getContext().getTransactionActivityIntentForHash("0xFOO"))

        onView(withId(R.id.nonce)).check(matches(withText("11")))
    }

    @Test
    fun isLabeledToWhenWeReceive() {
        val transaction = Transaction(ETH_IN_WEI, from = DEFAULT_TEST_ADDRESS, to = Room77, nonce = 11, txHash = "0xFOO12")
        TestApp.transactionProvider.addTransaction(TransactionWithState(transaction, TransactionState()))

        rule.launchActivity(InstrumentationRegistry.getContext().getTransactionActivityIntentForHash(transaction.txHash!!))

        onView(withId(R.id.from_to_title)).check(matches(withText(R.string.transaction_to_label)))
        onView(withId(R.id.from_to)).check(matches(withText("Room77")))
    }


    @Test
    fun isLabeledFromWhenWeReceive() {
        val transaction = Transaction(ETH_IN_WEI, from = ShapeShift, to = DEFAULT_TEST_ADDRESS, nonce = 11, txHash = "0xFOO21")
        TestApp.transactionProvider.addTransaction(TransactionWithState(transaction, TransactionState()))

        rule.launchActivity(InstrumentationRegistry.getContext().getTransactionActivityIntentForHash(transaction.txHash!!))

        onView(withId(R.id.from_to_title)).check(matches(withText(R.string.transaction_from_label)))
        onView(withId(R.id.from_to)).check(matches(withText("ShapeShift")))
    }


}
