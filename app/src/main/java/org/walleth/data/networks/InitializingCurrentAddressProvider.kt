package org.walleth.data.networks

import org.kethereum.model.Address
import org.walleth.data.config.Settings

class InitializingCurrentAddressProvider(settings: Settings) : CurrentAddressProvider(settings) {

    init {
        val lastAddress = settings.accountAddress
        if (lastAddress != null) {
            setCurrent(Address(lastAddress))
        }
    }

}