package org.walleth.tests

import android.support.test.espresso.Espresso.closeSoftKeyboard
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.activities.SelectReferenceActivity
import org.walleth.infrastructure.TestApp

class TheSelectReferenceActivity {

    @get:Rule
    var rule = TruleskActivityRule(SelectReferenceActivity::class.java)

    @Test
    fun listShows() {

        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()))

        rule.screenShot("reference_list")
    }

    @Test
    fun addDialogShows() {

        onView(withId(R.id.fab)).perform(click())

        onView(withId(R.id.reference_text)).check(matches(isDisplayed()))

        rule.screenShot("add_dialog")
    }

    @Test
    fun weCanAdd() {

        onView(withId(R.id.fab)).perform(click())

        assertThat(TestApp.fixedValueExchangeProvider.getAvailableFiatInfoMap().keys).doesNotContain("TST")

        closeSoftKeyboard()

        onView(withId(R.id.reference_text)).perform(typeText("TST"))
        onView(withText(android.R.string.ok)).perform(click())

        assertThat(TestApp.fixedValueExchangeProvider.getAvailableFiatInfoMap().keys).contains("TST")
    }
}
