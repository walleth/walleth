package org.walleth.tests

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.common.truth.Truth
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskIntentRule
import org.walleth.App
import org.walleth.R
import org.walleth.intents.IntentHandlerActivity
import org.walleth.intents.getEthereumViewIntent


class TheIntentHandlerActivity {

    @get:Rule
    var rule = TruleskIntentRule(IntentHandlerActivity::class.java, false)


    private fun launchWithURL(url: String) {
        rule.launchActivity(ApplicationProvider.getApplicationContext<App>().getEthereumViewIntent(url))
    }

    @Test
    fun handlesInvalidScannedStrings() {
        launchWithURL("The invalid string")
        onView(withText(R.string.create_tx_error_invalid_url_title)).check(matches(isDisplayed()))
        Truth.assertThat(rule.activity.isFinishing).isFalse()
    }

    @Test
    fun handlesAddressesWithValue() {
        launchWithURL("ethereum:0xdeadbeef?value=100000000000000")
        onView(allOf(withId(R.id.current_eth), isDescendantOfA(withId(R.id.amount_value))))
                .check(matches(withText("0.0001")))
    }

    @Test
    fun handlesWalletConnectURL() {
        launchWithURL("wc:f99e1c60-1b63-4b3b-9907-d22d9d3cdab8@1?bridge=https%3A%2F%2Fbridge.walletconnect.org&key=aa1dfb11a2aaee9c9a4c34dd8fb64454ddf95864330e8d7cd4ac2a2329e786c3")
        onView(withText(R.string.wallet_connect)).check(matches(isDisplayed()))
    }


}