package org.walleth.tests

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.espresso.matcher.ViewMatchers.Visibility.GONE
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.activities.RequestActivity

class TheRequestActivity {

    @get:Rule
    var rule = TruleskActivityRule(RequestActivity::class.java)

    @Test
    fun requestIsThereAndHasQRCodeAndNoValue() {
        onView(withId(R.id.receive_qrcode)).check(matches(isDisplayed()))
        onView(withId(R.id.add_value_checkbox)).check(matches(isNotChecked()))
        onView(withId(R.id.value_input_layout)).check(matches(withEffectiveVisibility(GONE)))

        rule.screenShot("transaction_no_value")
    }

    @Test
    fun thereIsNoValueEditShownWhenCheckboxUnchecked() {

        onView(withId(R.id.add_value_checkbox)).perform(click())
        onView(withId(R.id.add_value_checkbox)).check(matches(isChecked()))

        onView(withId(R.id.value_input_layout)).check(matches(isDisplayed()))

        onView(withId(R.id.value_input_edittext)).perform(typeText("0.42"))

        Espresso.closeSoftKeyboard()

        rule.screenShot("transaction_with_value")
    }

}
