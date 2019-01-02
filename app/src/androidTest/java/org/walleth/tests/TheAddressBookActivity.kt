package org.walleth.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.activities.AddressBookActivity

class TheAddressBookActivity {

    @get:Rule
    var rule = TruleskActivityRule(AddressBookActivity::class.java)

    @Test
    fun listShows() {

        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()))

        rule.screenShot("reference_list")
    }
}
