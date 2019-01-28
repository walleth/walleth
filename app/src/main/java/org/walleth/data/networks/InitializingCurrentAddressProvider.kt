package org.walleth.data.networks

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.kethereum.crypto.createEthereumKeyPair
import org.kethereum.model.Address
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.DEFAULT_PASSWORD
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.config.Settings
import org.walleth.data.keystore.WallethKeyStore

class InitializingCurrentAddressProvider(keyStore: WallethKeyStore, appDatabase: AppDatabase, settings: Settings, context: Context) : CurrentAddressProvider(settings) {

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