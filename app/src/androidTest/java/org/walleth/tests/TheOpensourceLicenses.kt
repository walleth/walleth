package org.walleth.tests

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskIntentRule


class TheOpensourceLicenses {


        @get:Rule
        var rule = TruleskIntentRule(OssLicensesMenuActivity::class.java)

        @Test
        fun rejectsCriticallyLongAddress() {
            rule.screenShot("licenses")
        }

}
