package org.walleth.tests

import android.content.Intent
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.ViewMatchers
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.activities.CreateTransactionActivity
import org.walleth.infrastructure.TestApp

class TheCreateTransactionActivity {

    @get:Rule
    var rule = TruleskActivityRule(CreateTransactionActivity::class.java, autoLaunch = false)

    @Test
    fun rejectsEmptyAddress() {
        rule.launchActivity()
        Espresso.onView(ViewMatchers.withId(R.id.fab)).perform(ViewActions.closeSoftKeyboard(), ViewActions.click())

        Espresso.onView(ViewMatchers.withText(R.string.create_tx_error_address_must_be_specified)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        rule.screenShot("address_empty")
        Truth.assertThat(rule.activity.isFinishing).isFalse()
    }

    @Test
    fun rejectsDifferentChainId() {
        rule.launchActivity(Intent.getIntentOld("ethereum:0x12345@"+(TestApp.mySettings.chain+1)))

        Espresso.onView(ViewMatchers.withText(R.string.wrong_network)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(R.string.please_switch_network)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        rule.screenShot("chainId_not_valid")
        Truth.assertThat(rule.activity.isFinishing).isFalse()
    }

    @Test
    fun acceptsDifferentChainId() {
        TestApp.networkDefinitionProvider
        rule.launchActivity(Intent.getIntentOld("ethereum:0x12345@"+TestApp.networkDefinitionProvider.getCurrent()))

        Espresso.onView(ViewMatchers.withText(R.string.wrong_network)).check(ViewAssertions.doesNotExist())
        Espresso.onView(ViewMatchers.withText(R.string.please_switch_network)).check(ViewAssertions.doesNotExist())

        rule.screenShot("chainId_valid")
        Truth.assertThat(rule.activity.isFinishing).isFalse()
    }

}