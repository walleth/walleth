package org.ligi.walleth

import android.app.Application
import org.ethereum.geth.AccountManager
import org.ethereum.geth.Geth
import java.io.File


class App : Application() {

    val keyStoreFile by lazy { File(filesDir, "keystore") }

    override fun onCreate() {
        super.onCreate()

        accountManager = AccountManager(keyStoreFile.absolutePath, Geth.LightScryptN, Geth.LightScryptP)
    }

    companion object {
        lateinit var accountManager: AccountManager
    }
}
