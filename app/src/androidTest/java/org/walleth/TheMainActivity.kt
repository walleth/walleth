package org.walleth

import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.ligi.walleth.App
import org.ligi.walleth.activities.MainActivity
import java.math.BigInteger

class TheMainActivity {

    @get:Rule
    var rule = TruleskActivityRule(MainActivity::class.java)

    @Test
    fun weCanEnterMainActivity() {
        rule.screenShot("main")
    }

    @Test
    fun exchangeRateIs() {
        TestApp.fixedValueExchangeProvider.exchangeRateMap["EUR"] = 10.0
        rule.screenShot("main")
    }

    @Test
    fun noSendIsVisibleAtBalanceZero() {
        TestApp.balanceProvider.setBalance(App.currentAddress!!,42,BigInteger("0"))
        rule.screenShot("main")
    }




}
