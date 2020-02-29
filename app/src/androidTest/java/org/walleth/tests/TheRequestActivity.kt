package org.walleth.tests

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.request.RequestActivity

class TheRequestActivity {

    @get:Rule
    var rule = TruleskActivityRule(RequestActivity::class.java)

    @Test
    fun requestIsThereAndHasQRCodeAndNoValue() {
        onView(withId(R.id.receive_qrcode)).check(matches(isDisplayed()))
        onView(withId(R.id.add_value_checkbox)).check(matches(isNotChecked()))
        onView(withId(R.id.value_input)).check(matches(withEffectiveVisibility(GONE)))

        rule.screenShot("transaction_no_value")
    }

    @Test
    fun thereIsNoValueEditShownWhenCheckboxUnchecked() {

        onView(withId(R.id.add_value_checkbox)).perform(click())
        onView(withId(R.id.add_value_checkbox)).check(matches(isChecked()))

        onView(withId(R.id.value_input)).check(matches(isDisplayed()))

        onView(withId(R.id.current_eth)).perform(replaceText("0.42"))

        Espresso.closeSoftKeyboard()

        rule.screenShot("transaction_with_value")
    }

}
