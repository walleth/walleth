package org.walleth.tests

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.activities.OfflineTransactionActivity
import org.walleth.infrastructure.TestApp

class TheOfflineTransactionActivity {

    val SIGNED_TX = "0xf86b808405f5e10082520894fdf1210fc262c73d0436236a0e07be419babbbc48813b4da79fd0e00008025a06b69466ca8fd9c82572b505407022e7a2c5d292b8f881ea6f7aaa15c1747c7dca039d4a32556ce058ff4724e346babe6342db2e3705f06cb9871da28ec4c1679cf"

    @get:Rule
    var rule = TruleskActivityRule(OfflineTransactionActivity::class.java, false)

    @Test
    fun activityShowsUp() {

        TestApp.testDatabase.transactions.deleteAll()
        rule.launchActivity()

        rule.screenShot("offline_transaction")

    }

    @Test
    fun handlesInvalidInputCorrectly() {

        rule.launchActivity()


        onView(withId(R.id.transaction_to_relay_hex)).perform(typeText("x"))

        onView(withId(R.id.fab)).perform(click())

        onView(withText(R.string.input_not_valid_title)).check(matches(isDisplayed()))

        rule.screenShot("offline_transaction_invalid_input")
    }

    @Test
    fun handlesSignedTransaction() {

        TestApp.testDatabase.transactions.deleteAll()

        rule.launchActivity()

        onView(withId(R.id.transaction_to_relay_hex)).perform(typeText(SIGNED_TX))

        onView(withId(R.id.fab)).perform(click())

        assertThat(TestApp.testDatabase.transactions.getTransactions().size).isEqualTo(1)
    }


}