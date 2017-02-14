package org.ligi.walleth

import android.app.Application
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import java.io.File


class App : Application() {

    val keyStoreFile by lazy { File(filesDir, "keystore") }

    override fun onCreate() {
        super.onCreate()

        keyStore = KeyStore(keyStoreFile.absolutePath, Geth.LightScryptN, Geth.LightScryptP)
    }

    companion object {
        lateinit var keyStore: KeyStore
    }
}
