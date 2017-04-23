package org.walleth

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.ligi.walleth.R
import org.ligi.walleth.activities.RequestActivity

class TheRequestActivity {

    @get:Rule
    var rule = TruleskActivityRule(RequestActivity::class.java)

    @Test
    fun requestIsThereAndHasQRCode() {
        rule.screenShot("balance_zero")
        onView(withId(R.id.receive_qrcode)).check(matches(isDisplayed()))
    }

}
