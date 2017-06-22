package org.walleth

import android.support.test.espresso.Espresso.closeSoftKeyboard
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import com.google.common.truth.Truth.assertThat
import kotlinx.android.synthetic.main.activity_account_create.*
import org.junit.Rule
import org.junit.Test
import org.kethereum.model.Address
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.activities.CreateAccountActivity
import org.walleth.infrastructure.TestApp

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
    fun rejects_blank_name() {

        onView(withId(R.id.hexInput)).perform(typeText("0xfdf1210fc262c73d0436236a0e07be419babbbc4"))

        closeSoftKeyboard()

        onView(withId(R.id.fab)).perform(click())

        onView(withText(R.string.alert_problem_title)).check(matches(isDisplayed()))
        onView(withText(R.string.please_enter_name)).check(matches(isDisplayed()))

        rule.screenShot("address_not_valid")
        assertThat(rule.activity.isFinishing).isFalse()
    }

    @Test
    fun when_creating_new_address_old_gets_cleared() {

        onView(withId(R.id.new_address_button)).perform(click())

        val firstCreatedAddress = Address(rule.activity.hexInput.text.toString())

        assertThat(TestApp.keyStore.hasKeyForForAddress(firstCreatedAddress)).isTrue()

        onView(withId(R.id.new_address_button)).perform(click())

        val secondCreatedAddress = Address(rule.activity.hexInput.text.toString())

        onView(withId(R.id.nameInput)).perform(typeText("nameProbe"))

        closeSoftKeyboard()

        onView(withId(R.id.fab)).perform(click())

        assertThat(TestApp.keyStore.hasKeyForForAddress(firstCreatedAddress)).isFalse()
        assertThat(TestApp.keyStore.hasKeyForForAddress(secondCreatedAddress)).isTrue()
        assertThat(firstCreatedAddress).isNotEqualTo(secondCreatedAddress)
    }

    @Test
    fun savesValidAddress() {

        onView(withId(R.id.hexInput)).perform(typeText("0xfdf1210fc262c73d0436236a0e07be419babbbc4"))
        onView(withId(R.id.nameInput)).perform(typeText("nameProbe"))
        onView(withId(R.id.noteInput)).perform(typeText("noteProbe"))

        closeSoftKeyboard()

        rule.screenShot("create")

        onView(withId(R.id.fab)).perform(click())

        val tested = TestApp.addressBookWithEntries.getEntryForName(Address("0xfdf1210fc262c73d0436236a0e07be419babbbc4"))

        assertThat(tested).isNotNull()
        assertThat(tested!!.name).isEqualTo("nameProbe")
        assertThat(tested.note).isEqualTo("noteProbe")
        assertThat(rule.activity.isFinishing).isTrue()
    }
}
