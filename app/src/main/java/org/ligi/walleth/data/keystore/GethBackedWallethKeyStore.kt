package org.ligi.walleth.data.keystore

import android.content.Context
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import org.ligi.walleth.data.WallethAddress
import org.ligi.walleth.data.toWallethAddress
import java.io.File

class GethBackedWallethKeyStore(context: Context) : WallethKeyStore {

    private val keyStoreFile by lazy { File(context.filesDir, "keystore") }
    val keyStore by lazy { KeyStore(keyStoreFile.absolutePath, Geth.LightScryptN, Geth.LightScryptP) }

    private var currentAddress: WallethAddress? = null

    override fun getCurrentAddress(): WallethAddress {
        if (currentAddress == null) {
            if (keyStore.accounts.size() > 0) {
                currentAddress = keyStore.accounts[0].address.toWallethAddress()
            } else {
                currentAddress = keyStore.newAccount("default").address.toWallethAddress()
            }

        }
        return currentAddress!!
    }

    override fun importKey(json: String, importPassword: String, newPassword: String)
            = keyStore.importKey(json.toByteArray(), importPassword, newPassword)?.address?.toWallethAddress()

    override fun exportCurrentKey(unlockPassword: String, exportPassword: String)
            = String(keyStore.exportKey(keyStore.accounts[0], unlockPassword, exportPassword))

}
