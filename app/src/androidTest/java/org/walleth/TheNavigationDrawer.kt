package org.walleth

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.DrawerActions.open
import android.support.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.activities.MainActivity
import org.walleth.data.addressbook.AddressBookEntry

class TheNavigationDrawer {

    @get:Rule
    var rule = TruleskActivityRule(MainActivity::class.java)


    @Test
    fun navigationDrawerIsUsuallyNotShown() {
        onView(withId(R.id.navigationView)).check(matches(not(isDisplayed())))
    }

    @Test
    fun navigationDrawerOpensWhenRequested() {
        onView(withId(R.id.drawer_layout)).perform(open())
        onView(withId(R.id.navigationView)).check(matches(isDisplayed()))
    }

    @Test
    fun testNameIsDisplayedCorrectly() {
        onView(withId(R.id.drawer_layout)).perform(open())

        rule.activity.runOnUiThread {
            TestApp.addressBookWithEntries.setEntry(AddressBookEntry("espresso ligi", TestApp.keyStore.getCurrentAddress()))
        }

        onView(withId(R.id.accountName)).check(matches(withText("espresso ligi")))

        rule.screenShot("drawer_opened")
    }


}
