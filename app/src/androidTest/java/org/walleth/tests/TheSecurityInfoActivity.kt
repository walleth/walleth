package org.walleth.tests

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withText
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.activities.SecurityInfoActivity

class TheSecurityInfoActivity {

    @get:Rule
    var rule = TruleskActivityRule(SecurityInfoActivity::class.java)

    @Test
    fun infoShows() {
        onView(withText(R.string.security_info)).check(matches(isDisplayed()))

        rule.screenShot("security_info")
    }

}
