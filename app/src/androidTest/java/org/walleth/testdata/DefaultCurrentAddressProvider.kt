package org.walleth.testdata

import org.kethereum.crypto.createEcKeyPair
import org.kethereum.crypto.getAddress
import org.kethereum.crypto.initializeCrypto
import org.kethereum.model.Address
import org.walleth.data.DEFAULT_PASSWORD
import org.walleth.data.config.Settings
import org.walleth.data.networks.CurrentAddressProvider

val DEFAULT_TEST_KEY = createEcKeyPair()
val DEFAULT_TEST_KEY2 = createEcKeyPair()
val DEFAULT_TEST_KEY3 = createEcKeyPair()

val DEFAULT_TEST_ADDRESS = Address(DEFAULT_TEST_KEY.getAddress())
val DEFAULT_TEST_ADDRESS2 = Address(DEFAULT_TEST_KEY2.getAddress())
val DEFAULT_TEST_ADDRESS3 = Address(DEFAULT_TEST_KEY3.getAddress())

class DefaultCurrentAddressProvider(settings: Settings, keyStore: TestKeyStore) : CurrentAddressProvider(settings) {

    init {
        initializeCrypto()
        keyStore.importKey(DEFAULT_TEST_KEY, DEFAULT_PASSWORD)
        keyStore.importKey(DEFAULT_TEST_KEY2, DEFAULT_PASSWORD)
        keyStore.importKey(DEFAULT_TEST_KEY3, DEFAULT_PASSWORD)
        setCurrent(DEFAULT_TEST_ADDRESS)

    }

}