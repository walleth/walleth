package org.walleth.tests

import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.accounts.ExportKeyActivity

class TheExportKeyActivity {

    @get:Rule
    var rule = TruleskActivityRule(ExportKeyActivity::class.java)

    @Test
    fun exportShows() {
        rule.screenShot("export")
    }

}
