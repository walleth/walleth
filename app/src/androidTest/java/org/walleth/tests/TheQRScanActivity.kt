package org.walleth.tests

import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters
import org.ligi.trulesk.TruleskActivityRule
import org.walleth.infrastructure.TestApp
import org.walleth.overview.OverviewActivity

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TheQRScanActivity {

    val currentNetwork = TestApp.chainInfoProvider.getCurrent()

    @get:Rule
    var rule = TruleskActivityRule(OverviewActivity::class.java, false)

    @Test
    fun onBoardingIsShown() {

        /*
"""
{"address":"9f179643133d1046e738c5ac3ac246085343092a","crypto":{"cipher":"aes-128-ctr","cipherparams":{"iv":"c8435ab546562262ea7279146ca5d92b"},"ciphertext":"b9abe24527a6cda4858ab6c5b52910d552a820664d99ab99e2b07a8bb875652d","kdf":"scrypt","kdfparams":{"dklen":32,"n":4096,"p":6,"r":8,"salt":"9ca0738a2b824bf2e753c009586c5ff7b54086039d221e850b6eaa85fa0e90dc"},"mac":"4e803b48278a3f95b7732b12d058785fbd064c6d0f92e9dc0b74adf23a9ac382"},"id":"511a6083-3e2f-49eb-94b1-9f3c1792fa6b","version":3}
"""
*/
    }

}
