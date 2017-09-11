package org.walleth.tests

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withText
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskIntentRule
import org.walleth.R
import org.walleth.activities.PreferenceActivity
import org.walleth.activities.SelectReferenceActivity


class ThePreferencesActivity {

    @get:Rule
    var rule = TruleskIntentRule(PreferenceActivity::class.java)

    @Test
    fun preferencesShow() {
        onView(withText(R.string.day_or_night_summary)).check(matches(isDisplayed()))

        rule.screenShot("preferences")
    }

    @Test
    fun whenClickOnSelectFiatWeGetToSelectFiat() {

        onView(withText(R.string.select_fiat_reference)).perform(click())

        intended(hasComponent(SelectReferenceActivity::class.java.name))

    }
}
