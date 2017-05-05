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
import org.walleth.activities.ImportActivity

class TheImportAccountActivity {

    @get:Rule
    var rule = TruleskActivityRule(ImportActivity::class.java) {
        TestApp.balanceProvider.reset()
    }

    @Test
    fun importShows() {
        onView(withId(R.id.password)).check(matches(isDisplayed()))
        onView(withId(R.id.account_name)).check(matches(isDisplayed()))
        onView(withId(R.id.inport_json_text)).check(matches(isDisplayed()))

        rule.screenShot("import")
    }

    @Test
    fun happyPathWorks() {

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

        closeSoftKeyboard()

        onView(withId(R.id.fab)).perform(click())

        val accountName = TestApp.addressBookWithEntries.getEntryForName(TestApp.keyStore.import_result_address).name

        assertThat(accountName).isEqualTo("Imported")
    }

    @Test
    fun weCanChangeTheName() {

        onView(withId(R.id.account_name)).perform(typeText("new name"))

        closeSoftKeyboard()

        onView(withId(R.id.fab)).perform(click())

        val accountName = TestApp.addressBookWithEntries.getEntryForName(TestApp.keyStore.import_result_address).name

        assertThat(accountName).isEqualTo("new name")
    }

}
