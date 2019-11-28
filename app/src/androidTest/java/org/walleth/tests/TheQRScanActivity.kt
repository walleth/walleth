package org.walleth.tests

import android.Manifest
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.linkedin.android.testbutler.TestButler
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskIntentRule
import org.walleth.R
import org.walleth.qr.scan.QRScanActivity

class TheQRScanActivity {

    @get:Rule
    var rule = TruleskIntentRule(QRScanActivity::class.java)

    @Test
    fun thatScanInstructionsShow() {

        TestButler.grantPermission(ApplicationProvider.getApplicationContext(), Manifest.permission.CAMERA)

        onView(withText(R.string.scan_instructions)).check(matches(isDisplayed()))

        rule.screenShot("QR_Scan")
    }

}
