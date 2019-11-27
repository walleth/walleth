package org.walleth.tests

import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskIntentRule
import org.walleth.qr.scan.QRScanActivity

class TheQRScanActivity {

    @get:Rule
    var rule = TruleskIntentRule(QRScanActivity::class.java)

    @Test
    fun preferencesShow() {

        rule.screenShot("QR_Scan")
    }

}
