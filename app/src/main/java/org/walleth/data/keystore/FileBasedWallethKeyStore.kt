package org.walleth.data.keystore

import android.content.Context
import org.kethereum.crypto.ECKeyPair
import org.kethereum.model.Address
import org.kethereum.wallet.LIGHT_SCRYPT_CONFIG
import org.kethereum.wallet.generateWalletFile
import org.kethereum.wallet.loadKeysFromWalletFile
import org.walleth.data.DEFAULT_PASSWORD
import java.io.File

class FileBasedWallethKeyStore(val context: Context) : WallethKeyStore {

    private val keyStoreDirectory by lazy {
        File(context.filesDir, "keystore").also {
            it.mkdirs()
        }
    }

    private val fileMap by lazy {
        mutableMapOf<Address, File>().apply {
            rebuildList()
        }
    }

    private fun MutableMap<Address, File>.rebuildList() {
        keyStoreDirectory.listFiles().forEach {
            put(Address(it.name.split("--").last().removeSuffix(".json")), it)
        }
    }

    override fun hasKeyForForAddress(wallethAddress: Address) =
            fileMap.containsKey(wallethAddress)

    override fun getKeyForAddress(address: Address, password: String) =
            fileMap[address]?.loadKeysFromWalletFile(password)

    override fun deleteKey(address: Address) = (fileMap[address]?.delete() ?: false).also {
        fileMap.remove(address)
    }

    override fun getAddresses() = fileMap.keys

    override fun importKey(key: ECKeyPair, password: String) =
            key.generateWalletFile(DEFAULT_PASSWORD, keyStoreDirectory, LIGHT_SCRYPT_CONFIG).wallet.address?.let {
                fileMap.rebuildList()
                Address(it)
            }

}
