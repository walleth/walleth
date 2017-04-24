package org.walleth

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.DrawerActions.open

import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.espresso.matcher.ViewMatchers.Visibility.*
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.ligi.walleth.App
import org.ligi.walleth.R
import org.ligi.walleth.activities.MainActivity
import org.ligi.walleth.data.ETH_IN_WEI
import java.math.BigInteger

class TheMainActivity {

    @get:Rule
    var rule = TruleskActivityRule(MainActivity::class.java)

    @Test
    fun behavesCorrectlyWhenBalanceIsZero() {
        TestApp.balanceProvider.setBalance(App.currentAddress!!,42,BigInteger("0"))

        onView(withId(R.id.current_eth)).check(matches(withText("0")))

        onView(withId(R.id.send_container)).check(matches(withEffectiveVisibility(INVISIBLE)))
        onView(withId(R.id.empty_view)).check(matches(withEffectiveVisibility(VISIBLE)))

        onView(withId(R.id.transactionRecyclerIn)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.transactionRecyclerOut)).check(matches(withEffectiveVisibility(GONE)))

        onView(withId(R.id.fab)).check(matches(withEffectiveVisibility(GONE)))

        rule.screenShot("balance_zero")
    }

    @Test
    fun behavesCorrectlyWhenBalanceIsOne() {
        TestApp.balanceProvider.setBalance(App.currentAddress!!,42, ETH_IN_WEI)

        onView(withId(R.id.current_eth)).check(matches(withText("1")))

        onView(withId(R.id.send_container)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.empty_view)).check(matches(withEffectiveVisibility(GONE)))

        onView(withId(R.id.transactionRecyclerIn)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.transactionRecyclerOut)).check(matches(withEffectiveVisibility(VISIBLE)))

        onView(withId(R.id.fab)).check(matches(withEffectiveVisibility(VISIBLE)))

        rule.screenShot("balance_one")
    }

    @Test
    fun navigationDrawerIsUsuallyNotShown() {
        onView(withId(R.id.navigationView)).check(matches(not(isDisplayed())))
    }

    @Test
    fun navigationDrawerOpensWhenRequested() {
        onView(withId(R.id.drawer_layout)).perform(open())
        onView(withId(R.id.navigationView)).check(matches(isDisplayed()))

        rule.screenShot("drawer_opened")
    }


}
