package org.walleth.testdata

import org.kethereum.model.Address
import org.walleth.data.config.Settings
import org.walleth.data.networks.CurrentAddressProvider

val DEFAULT_TEST_ADDRESS = Address("0xfdf1210fc262c73d0436236a0e07be419babbbc4")
val DEFAULT_TEST_ADDRESS2 = Address("0xfdf1210fc262c73d0436236a0e07be419babbbc5")
val DEFAULT_TEST_ADDRESS3 = Address("0xfdf1210fc262c73d0436236a0e07be419babbbc6")

class DefaultCurrentAddressProvider(settings: Settings) : CurrentAddressProvider(settings) {

    init {
        setCurrent(DEFAULT_TEST_ADDRESS)
    }

}