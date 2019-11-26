package org.walleth.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.info.WallETHInfoActivity

class TheWallETHInfoActivity {

    @get:Rule
    var rule = TruleskActivityRule(WallETHInfoActivity::class.java)

    @Test
    fun infoShows() {
        onView(withId(R.id.intro_text)).check(matches(isDisplayed()))

        rule.screenShot("info")
    }

}
