package org.walleth.tests

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.espresso.matcher.ViewMatchers.Visibility.*
import android.support.v7.app.AppCompatDelegate
import com.github.amlcurran.showcaseview.ShowcaseView
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters
import org.ligi.trulesk.TruleskActivityRule
import org.mockito.Mockito.`when`
import org.walleth.R
import org.walleth.activities.MainActivity
import org.walleth.data.ETH_IN_WEI
import org.walleth.data.balances.Balance
import org.walleth.data.tokens.getEthTokenForChain
import org.walleth.infrastructure.TestApp
import org.walleth.testdata.loadTestData
import java.math.BigInteger.ZERO

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TheMainActivity {

    val currentNetwork = TestApp.networkDefinitionProvider.getCurrent()

    @get:Rule
    var rule = TruleskActivityRule(MainActivity::class.java, false)

    @Test
    fun onBoardingIsShown() {

        TestApp.testDatabase.runInTransaction {
            TestApp.testDatabase.balances.deleteAll()
            TestApp.testDatabase.transactions.deleteAll()
            TestApp.testDatabase.balances.upsert(Balance(TestApp.currentAddressProvider.getCurrent(), getEthTokenForChain(currentNetwork).address, currentNetwork.chain, 42, ZERO))
        }

        `when`(TestApp.mySettings.startupWarningDone).thenReturn(false)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        rule.launchActivity()

        onView(withText(R.string.onboarding_warning_message)).check(matches(isDisplayed()))
        onView(withClassName(containsString(ShowcaseView::class.java.name))).check(doesNotExist())
        rule.screenShot("warning")

        onView(withText(android.R.string.ok)).perform(click())

        onView(withClassName(containsString(ShowcaseView::class.java.name))).check(matches(isDisplayed()))

        rule.screenShot("warning")


    }

    @Test
    fun onBoardingIsNotShown() {

        `when`(TestApp.mySettings.startupWarningDone).thenReturn(true)

        rule.launchActivity()

        onView(withText(R.string.onboarding_warning_message)).check(doesNotExist())
    }

    @Test
    fun behavesCorrectlyNoTransactions() {


        TestApp.testDatabase.runInTransaction {
            TestApp.testDatabase.balances.deleteAll()
            TestApp.testDatabase.transactions.deleteAll()
            TestApp.testDatabase.balances.upsert(Balance(TestApp.currentAddressProvider.getCurrent(), getEthTokenForChain(currentNetwork).address, currentNetwork.chain, 42, ZERO))
        }

        rule.launchActivity()

        onView(allOf(isDescendantOfA(withId(R.id.value_view)), withId(R.id.current_eth)))
                .check(matches(withText("0")))

        onView(withId(R.id.send_container)).check(matches(withEffectiveVisibility(INVISIBLE)))
        onView(withId(R.id.empty_view)).check(matches(withEffectiveVisibility(VISIBLE)))

        onView(withId(R.id.transaction_recycler_in)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.transaction_recycler_out)).check(matches(withEffectiveVisibility(GONE)))

        rule.screenShot("no_transactions")
    }

    @Test
    fun behavesCorrectlyWhenBalanceIsOneWithTransactions() {

        TestApp.testDatabase.runInTransaction {
            TestApp.testDatabase.balances.deleteAll()
            TestApp.testDatabase.transactions.deleteAll()
            TestApp.testDatabase.transactions.loadTestData(currentNetwork.chain)
            TestApp.testDatabase.balances.upsert(Balance(TestApp.currentAddressProvider.getCurrent(), getEthTokenForChain(currentNetwork).address, currentNetwork.chain, 42, ETH_IN_WEI))
        }
        rule.launchActivity()

        onView(allOf(isDescendantOfA(withId(R.id.value_view)), withId(R.id.current_eth)))
                .check(matches(withText("1")))

        onView(withId(R.id.send_container)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.empty_view)).check(matches(withEffectiveVisibility(GONE)))

        onView(withId(R.id.transaction_recycler_in)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.transaction_recycler_out)).check(matches(withEffectiveVisibility(VISIBLE)))

        rule.screenShot("balance_one")
    }

}
