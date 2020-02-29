package org.walleth.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.accounts.ImportKeyActivity

class TheImportActivity {

    @get:Rule
    var rule = TruleskActivityRule(ImportKeyActivity::class.java)

    @Test
    fun importShows() {
        rule.screenShot("import")
    }

    @Test
    fun passwordShowsForWordList() {
        onView(withId(R.id.type_wordlist_select)).perform(click())
        rule.screenShot("import_wordlist")

        onView(withId(R.id.password)).check(matches(isDisplayed()))
    }

    @Test
    fun passwordShowsForJSON() {
        onView(withId(R.id.type_json_select)).perform(click())
        rule.screenShot("import_json")

        onView(withId(R.id.password)).check(matches(isDisplayed()))
    }

    @Test
    fun passwordDoesNotShowsForECDSA() {
        onView(withId(R.id.type_ecdsa_select)).perform(click())
        rule.screenShot("import_ecdsa")

        onView(withId(R.id.password)).check(matches(not(isDisplayed())))
    }

    @Test
    fun rejectsTooShortHexForECDSA() {
        onView(withId(R.id.type_ecdsa_select)).perform(click())
        onView(withId(R.id.key_content)).perform(replaceText("aa"))
        onView(withId(R.id.fab)).perform(click())

        onView(withText(R.string.key_length_error)).check(matches(isDisplayed()))
    }

    @Test
    fun rejectsTooLongtHexForECDSA() {
        onView(withId(R.id.type_ecdsa_select)).perform(click())
        onView(withId(R.id.key_content)).perform(replaceText("b".repeat(70)))
        onView(withId(R.id.fab)).perform(click())

        onView(withText(R.string.key_length_error)).check(matches(isDisplayed()))
    }

    @Test
    fun rejectsInvalidHexForECDSA() {
        onView(withId(R.id.type_ecdsa_select)).perform(click())
        onView(withId(R.id.key_content)).perform(replaceText("nopenopenopenopenopenopenopenopenopenopenopenopenopenopenopenope"))
        onView(withId(R.id.fab)).perform(click())

        onView(withText(R.string.dialog_title_error)).check(matches(isDisplayed()))
    }

    @Test
    fun acceptsCorrectECDSA() {
        onView(withId(R.id.type_ecdsa_select)).perform(click())
        onView(withId(R.id.key_content)).perform(replaceText("a".repeat(64)))
        onView(withId(R.id.fab)).perform(click())

        onView(withText(R.string.import_as_subtitle)).check(matches(isDisplayed()))
    }

}
