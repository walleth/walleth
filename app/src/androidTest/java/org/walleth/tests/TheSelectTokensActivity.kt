package org.walleth.tests

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.activities.SelectTokenActivity

class TheSelectTokensActivity {

    @get:Rule
    var rule = TruleskActivityRule(SelectTokenActivity::class.java)

    @Test
    fun listShows() {

        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()))

        rule.screenShot("token_select")
    }

}