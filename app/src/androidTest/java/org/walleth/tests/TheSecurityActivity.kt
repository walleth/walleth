package org.walleth.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.R
import org.walleth.security.SecurityActivity
import org.walleth.security.SecurityInfoFragment

class TheSecurityActivity {

    @get:Rule
    var rule = TruleskActivityRule(SecurityActivity::class.java)

    @Test
    fun infoShows() {
        onView(withText(R.string.info)).check(matches(isDisplayed()))
        onView(withText(R.string.rpc)).check(matches(isDisplayed()))
        onView(withText(R.string.sourcify)).check(matches(isDisplayed()))

        rule.screenShot("security_info")
    }

}
