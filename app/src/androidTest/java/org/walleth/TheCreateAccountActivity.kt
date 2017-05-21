package org.walleth

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
import org.walleth.activities.CreateAccountActivity
import org.walleth.data.WallethAddress

class TheCreateAccountActivity {

    @get:Rule
    var rule = TruleskActivityRule(CreateAccountActivity::class.java)

    @Test
    fun rejectsInvalidAddress() {

        onView(withId(R.id.fab)).perform(click())

        onView(withText(R.string.alert_problem_title)).check(matches(isDisplayed()))
        onView(withText(R.string.address_not_valid)).check(matches(isDisplayed()))

        rule.screenShot("address_not_valid")
        assertThat(rule.activity.isFinishing).isFalse()
    }


    @Test
    fun savesValidAddress() {

        onView(withId(R.id.hexInput)).perform(typeText("0xF00"))
        onView(withId(R.id.nameInput)).perform(typeText("nameProbe"))
        onView(withId(R.id.noteInput)).perform(typeText("noteProbe"))

        closeSoftKeyboard()

        rule.screenShot("create")

        onView(withId(R.id.fab)).perform(click())

        val tested = TestApp.addressBookWithEntries.getEntryForName(WallethAddress("0xF00"))

        assertThat(tested).isNotNull()
        assertThat(tested.name).isEqualTo("nameProbe")
        assertThat(tested.note).isEqualTo("noteProbe")
        assertThat(rule.activity.isFinishing).isTrue()
    }
}
