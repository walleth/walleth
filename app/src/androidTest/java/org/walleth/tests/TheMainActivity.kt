package org.walleth.tests

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.espresso.matcher.ViewMatchers.Visibility.*
import org.hamcrest.Matchers.allOf
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.activities.MainActivity
import org.walleth.data.ETH_IN_WEI
import org.walleth.data.balances.Balance
import org.walleth.data.tokens.getEthTokenForChain
import org.walleth.infrastructure.TestApp
import java.math.BigInteger.ZERO

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TheMainActivity {

    val currentNetwork = TestApp.networkDefinitionProvider.getCurrent()

    @get:Rule
    var rule = TruleskActivityRule(MainActivity::class.java) {
        TestApp.testDatabase.balances.deleteAll()

        TestApp.transactionProvider.reset()
    }

    @Test
    fun activityStarts() {
        // TODO investigate why when we delete this test the other ones fail - weird room problem
        rule.screenShot("balance_one")
    }

    @Test
    fun behavesCorrectlyNoTransactions() {
        TestApp.testDatabase.balances.upsert(Balance(TestApp.currentAddressProvider.getCurrent(), getEthTokenForChain(currentNetwork).address, currentNetwork.chain, 42, ZERO))

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

        TestApp.transactionProvider.load()
        TestApp.testDatabase.balances.upsert(Balance(TestApp.currentAddressProvider.getCurrent(), getEthTokenForChain(currentNetwork).address, currentNetwork.chain, 42, ETH_IN_WEI))

        onView(allOf(isDescendantOfA(withId(R.id.value_view)), withId(R.id.current_eth)))
                .check(matches(withText("1")))

        onView(withId(R.id.send_container)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.empty_view)).check(matches(withEffectiveVisibility(GONE)))

        onView(withId(R.id.transaction_recycler_in)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.transaction_recycler_out)).check(matches(withEffectiveVisibility(VISIBLE)))

        rule.screenShot("balance_one")
    }

}
