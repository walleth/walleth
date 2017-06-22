package org.walleth.data.keystore

import android.content.Context
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.ethereum.geth.Account
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import org.kethereum.functions.fromHexToByteArray
import org.kethereum.model.Address
import org.walleth.data.DEFAULT_PASSWORD
import org.walleth.data.SimpleObserveable
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.toKethereumAddress
import java.io.File

class GethBackedWallethKeyStore(val context: Context) : SimpleObserveable(), WallethKeyStore {

    private val keyStoreFile by lazy { File(context.filesDir, "keystore") }
    val keyStore by lazy { KeyStore(keyStoreFile.absolutePath, Geth.LightScryptN, Geth.LightScryptP) }

    val addressBook: AddressBook by LazyKodein(context.appKodein).instance()

    private var currentAddress: Address? = null

    override fun getCurrentAddress(): Address {
        if (currentAddress == null) {
            if (keyStore.accounts.size() > 0) {
                currentAddress = keyStore.accounts[0].address.toKethereumAddress()
            } else {
                currentAddress = keyStore.newAccount(DEFAULT_PASSWORD).address.toKethereumAddress()
                addressBook.setEntry(AddressBookEntry(
                        name = "Default Account",
                        address = currentAddress!!,
                        note = "This Account was created for you when WALLΞTH started for the first time",
                        isNotificationWanted = true
                ))
            }

        }
        return currentAddress!!
    }

    override fun setCurrentAddress(address: Address) {
        currentAddress = address
        promoteChange()
    }

    override fun newAddress(password: String) =
            keyStore.newAccount(password).address.toKethereumAddress()

    fun getAccountForAddress(wallethAddress: Address): Account? {
        val index = (0..(keyStore.accounts.size() - 1)).firstOrNull { keyStore.accounts.get(it).address.hex.equals(wallethAddress.hex, ignoreCase = true) }

        return if (index != null)
            keyStore.accounts.get(index)
        else
            null
    }

    override fun hasKeyForForAddress(wallethAddress: Address)
            = getAccountForAddress(wallethAddress) != null

    override fun deleteKey(address: Address, password: String) {
        getAccountForAddress(address)?.let {
            keyStore.deleteAccount(it, password)
        }
    }

    override fun importECDSAKey(key: String, storePassword: String)
            = keyStore.importECDSAKey(fromHexToByteArray(key), storePassword)?.address?.toKethereumAddress()

    override fun importJSONKey(json: String, importPassword: String, storePassword: String)
            = keyStore.importKey(json.toByteArray(), importPassword, storePassword)?.address?.toKethereumAddress()

    override fun exportCurrentKey(unlockPassword: String, exportPassword: String)
            = String(keyStore.exportKey(getAccountForAddress(currentAddress!!), unlockPassword, exportPassword))

}
