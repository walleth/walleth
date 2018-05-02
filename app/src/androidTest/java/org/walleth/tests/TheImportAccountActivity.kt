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
import org.kethereum.model.Address
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.activities.ImportActivity
import org.walleth.infrastructure.TestApp

class TheImportAccountActivity {

    @get:Rule
    var rule = TruleskActivityRule(ImportActivity::class.java) {
        TestApp.testDatabase.balances.deleteAll()
        TestApp.testDatabase.addressBook.deleteAll()
        TestApp.keyStore.addresses.clear()
    }

    @Test
    fun importShows() {
        onView(withId(R.id.password)).check(matches(isDisplayed()))
        onView(withId(R.id.account_name)).check(matches(isDisplayed()))
        onView(withId(R.id.key_content)).check(matches(isDisplayed()))

        rule.screenShot("import")
    }

    @Test
    fun happyPathWorks() {
        onView(withId(R.id.key_content)).perform(typeText("f8edd4c61011a63a55ba65096fed91889202b778435e436e71c545daf03ccb6a"))

        onView(withId(R.id.password)).perform(typeText("fortest12345"))

        onView(withId(R.id.type_ecdsa_select)).perform(click())

        closeSoftKeyboard()

        onView(withId(R.id.fab)).perform(click())

        onView(withText(R.string.dialog_title_success)).check(matches(isDisplayed()))

        rule.screenShot("import_success")
    }

    @Test
    fun badPasswordIsRejected() {

        onView(withId(R.id.password)).perform(typeText("bad password"))

        closeSoftKeyboard()

        onView(withId(R.id.fab)).perform(click())

        onView(withText(R.string.dialog_title_error)).check(matches(isDisplayed()))

        rule.screenShot("import_bad_password")
    }

    @Test
    fun whenNoNameWasEnteredItDefaultsToImported() {

        onView(withId(R.id.key_content)).perform(typeText("f8edd4c61011a63a55ba65096fed91889202b778435e436e71c545daf03ccb6a"))

        onView(withId(R.id.password)).perform(typeText("fortest12345"))
        onView(withId(R.id.type_ecdsa_select)).perform(click())
        closeSoftKeyboard()

        onView(withId(R.id.fab)).perform(click())

        val accountName = TestApp.testDatabase.addressBook.byAddress(Address("0xdE7d734537Ed6776aDB01bA7a5d9A4Aa579d8Bc1"))?.name

        assertThat(accountName).isEqualTo("Imported")
    }

    @Test
    fun weCanChangeTheName() {


        onView(withId(R.id.key_content)).perform(typeText("f8edd4c61011a63a55ba65096fed91889202b778435e436e71c545daf03ccb6a"))

        onView(withId(R.id.password)).perform(typeText("fortest12345"))

        onView(withId(R.id.account_name)).perform(typeText("new name"))
        onView(withId(R.id.type_ecdsa_select)).perform(click())
        closeSoftKeyboard()

        onView(withId(R.id.fab)).perform(click())

        val accountName = TestApp.testDatabase.addressBook.byAddress(Address("0xdE7d734537Ed6776aDB01bA7a5d9A4Aa579d8Bc1"))?.name

        assertThat(accountName).isEqualTo("new name")
    }

}
