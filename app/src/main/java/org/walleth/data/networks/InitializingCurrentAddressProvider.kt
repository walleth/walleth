package org.walleth.data.networks

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.kethereum.keystore.api.KeyStore
import org.kethereum.model.Address
import org.walleth.data.AppDatabase
import org.walleth.data.config.Settings

class InitializingCurrentAddressProvider(keyStore: KeyStore, appDatabase: AppDatabase, settings: Settings, context: Context) : CurrentAddressProvider(settings) {

    init {
        val lastAddress = settings.accountAddress
        if (lastAddress != null) {
            setCurrent(Address(lastAddress))
        } else {
            GlobalScope.async(Dispatchers.Default) {
                if (!keyStore.getAddresses().isEmpty()) {
                    postValue(keyStore.getAddresses().first())
                }
            }
        }
    }

}