package org.walleth.testdata

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kethereum.crypto.createEthereumKeyPair
import org.kethereum.crypto.toAddress
import org.walleth.data.DEFAULT_PASSWORD
import org.walleth.data.config.Settings
import org.walleth.data.addresses.CurrentAddressProvider

val DEFAULT_TEST_KEY = createEthereumKeyPair()
val DEFAULT_TEST_KEY2 = createEthereumKeyPair()
val DEFAULT_TEST_KEY3 = createEthereumKeyPair()

val DEFAULT_TEST_ADDRESS = DEFAULT_TEST_KEY.toAddress()
val DEFAULT_TEST_ADDRESS2 = DEFAULT_TEST_KEY2.toAddress()
val DEFAULT_TEST_ADDRESS3 = DEFAULT_TEST_KEY3.toAddress()

class DefaultCurrentAddressProvider(settings: Settings, keyStore: TestKeyStore) : CurrentAddressProvider(settings) {

    init {
        keyStore.addKey(DEFAULT_TEST_KEY, DEFAULT_PASSWORD, true)
        keyStore.addKey(DEFAULT_TEST_KEY2, DEFAULT_PASSWORD, true)
        keyStore.addKey(DEFAULT_TEST_KEY3, DEFAULT_PASSWORD, true)

        GlobalScope.launch {
            setCurrent(DEFAULT_TEST_ADDRESS)
        }
    }

}