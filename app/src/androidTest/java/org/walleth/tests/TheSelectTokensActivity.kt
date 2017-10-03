package org.walleth.tests

import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.activities.SelectTokenActivity

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TheSelectTokensActivity {

    @get:Rule
    var rule = TruleskActivityRule(SelectTokenActivity::class.java) {
    }

    @Test
    fun activityStarts() {
        rule.screenShot("token_select")
    }

}