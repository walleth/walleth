package org.walleth.data.networks

import android.content.Context
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
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
            value = Address(lastAddress)
        } else {
            async(CommonPool) {
                if (keyStore.getAddressCount() > 0) {
                    postValue(keyStore.getAddressByIndex(0))
                } else {
                    val newAccountAddress = keyStore.newAddress(DEFAULT_PASSWORD)
                    postValue(newAccountAddress)
                    appDatabase.addressBook.upsert(AddressBookEntry(
                            name = context.getString (org.walleth.R.string.default_account_name),
                            address = newAccountAddress,
                            note = context.getString(R.string.new_address_note),
                            isNotificationWanted = true,
                            trezorDerivationPath = null
                    ))
                }
            }
        }
    }

}