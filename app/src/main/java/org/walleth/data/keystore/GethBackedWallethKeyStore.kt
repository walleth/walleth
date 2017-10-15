package org.walleth.data.keystore

import android.content.Context
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import org.kethereum.model.Address
import org.walleth.kethereum.geth.toKethereumAddress
import org.walleth.khex.hexToByteArray
import java.io.File

class GethBackedWallethKeyStore(val context: Context) : WallethKeyStore {

    private val keyStoreFile = File(context.filesDir, "keystore")
    val keyStore = KeyStore(keyStoreFile.absolutePath, Geth.LightScryptN, Geth.LightScryptP)

    override fun newAddress(password: String) =
            keyStore.newAccount(password).address.toKethereumAddress()

    private fun getAccountForAddress(wallethAddress: Address)= getIndexForAddress(wallethAddress)?.let { keyStore.accounts.get(it) }

    private fun getIndexForAddress(wallethAddress: Address) =
            (0..(keyStore.accounts.size() - 1)).firstOrNull { keyStore.accounts.get(it).address.hex.equals(wallethAddress.hex, ignoreCase = true) }

    override fun hasKeyForForAddress(wallethAddress: Address)
            = getAccountForAddress(wallethAddress) != null

    override fun deleteKey(address: Address, password: String) {
        getAccountForAddress(address)?.let {
            keyStore.deleteAccount(it, password)
        }
    }

    override fun importECDSAKey(key: String, storePassword: String)
            = keyStore.importECDSAKey(key.hexToByteArray(), storePassword)?.address?.toKethereumAddress()

    override fun importJSONKey(json: String, importPassword: String, storePassword: String)
            = keyStore.importKey(json.toByteArray(), importPassword, storePassword)?.address?.toKethereumAddress()

    override fun exportKey(address: Address, unlockPassword: String, exportPassword: String)
            = String(keyStore.exportKey(getAccountForAddress(address), unlockPassword, exportPassword))

    override fun getAddressCount() = keyStore.accounts.size().toInt()

    override fun getAddressByIndex(index: Int): Address = keyStore.accounts.get(index.toLong()).address.toKethereumAddress()

}
