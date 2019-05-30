package org.walleth.tests

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.common.truth.Truth
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskIntentRule
import org.walleth.App
import org.walleth.R
import org.walleth.activities.IntentHandlerActivity
import org.walleth.activities.getEthereumViewIntent


class TheIntentHandlerActivity {

    @get:Rule
    var rule = TruleskIntentRule(IntentHandlerActivity::class.java, false)

    @Test
    fun handlesInvalidScannedStrings() {
        rule.launchActivity(ApplicationProvider.getApplicationContext<App>().getEthereumViewIntent("The invalid string"))
        Espresso.onView(withText(R.string.create_tx_error_invalid_url_title)).check(matches(isDisplayed()))
        Truth.assertThat(rule.activity.isFinishing).isFalse()
    }

    @Test
    fun handlesAddressesWithValue() {
        rule.launchActivity(ApplicationProvider.getApplicationContext<App>().getEthereumViewIntent("ethereum:0xdeadbeef?value=100000000000000"))
        Espresso.onView(allOf(withId(R.id.current_eth), isDescendantOfA(withId(R.id.amount_value))))
                .check(matches(withText("0.0001")))
    }
}