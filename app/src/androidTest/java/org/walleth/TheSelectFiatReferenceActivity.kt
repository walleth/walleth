package org.walleth

import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.ligi.walleth.activities.SelectFiatReferenceActivity

class TheSelectFiatReferenceActivity {

    @get:Rule
    var rule = TruleskActivityRule(SelectFiatReferenceActivity::class.java)

    @Test
    fun showsList() {
        rule.screenShot("select_fiat")
    }

}
