package org.walleth.tests

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskIntentRule
import org.walleth.R
import org.walleth.activities.IntentHandlerActivity
import org.walleth.activities.getEthereumViewIntent


class TheIntentHandlerActivity {

    @get:Rule
    var rule = TruleskIntentRule(IntentHandlerActivity::class.java, false)

    @Test
    fun handlesInvalidScannedStrings() {
        rule.launchActivity(InstrumentationRegistry.getTargetContext().getEthereumViewIntent("Ðereum string"))
        Espresso.onView(withText(R.string.create_tx_error_invalid_erc67_title)).check(matches(isDisplayed()))
        Truth.assertThat(rule.activity.isFinishing).isFalse()
    }

    @Test
    fun handlesAddressesWithValue() {
        rule.launchActivity(InstrumentationRegistry.getTargetContext().getEthereumViewIntent("ethereum:0xdeadbeef?value=1"))
        Espresso.onView(withId(R.id.amount_input)).check(matches(withText("0.000000000000000001")))
    }
}