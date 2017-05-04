package org.walleth.data.keystore

import android.content.Context
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import org.walleth.data.SimpleObserveable
import org.walleth.data.WallethAddress
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.toWallethAddress
import java.io.File

class GethBackedWallethKeyStore(val context: Context) : SimpleObserveable(), WallethKeyStore {

    private val keyStoreFile by lazy { File(context.filesDir, "keystore") }
    val keyStore by lazy { KeyStore(keyStoreFile.absolutePath, Geth.LightScryptN, Geth.LightScryptP) }

    val addressBook: AddressBook by LazyKodein(context.appKodein).instance()

    private var currentAddress: WallethAddress? = null

    override fun getCurrentAddress(): WallethAddress {
        if (currentAddress == null) {
            if (keyStore.accounts.size() > 0) {
                currentAddress = keyStore.accounts[0].address.toWallethAddress()
            } else {
                currentAddress = keyStore.newAccount("default").address.toWallethAddress()
                addressBook.setEntry(AddressBookEntry("Default Account", currentAddress!!, "This Account was created for you when WALLΞTH started for the first time"))
            }

        }
        return currentAddress!!
    }

    override fun setCurrentAddress(address: WallethAddress) {
        currentAddress = address
        promoteChange()
    }

    override fun importKey(json: String, importPassword: String, newPassword: String)
            = keyStore.importKey(json.toByteArray(), importPassword, newPassword)?.address?.toWallethAddress()

    override fun exportCurrentKey(unlockPassword: String, exportPassword: String)
            = String(keyStore.exportKey(keyStore.accounts[0], unlockPassword, exportPassword))

}
