package org.ligi.walleth

import android.app.Application
import android.content.Intent
import android.support.v7.app.AppCompatDelegate
import com.github.salomonbrys.kodein.*
import com.jakewharton.threetenabp.AndroidThreeTen
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import org.greenrobot.eventbus.EventBus
import org.ligi.tracedroid.TraceDroid
import org.ligi.walleth.core.GethLightEthereumService
import org.ligi.walleth.data.*
import org.ligi.walleth.data.addressbook.AddressBook
import org.ligi.walleth.data.addressbook.FileBackedAddressBook
import org.ligi.walleth.data.networks.NetworkDefinition
import org.ligi.walleth.data.networks.RinkebyNetworkDefinition
import org.ligi.walleth.data.syncprogress.SyncProgressProvider
import java.io.File

open class App : Application(), KodeinAware {

    override val kodein by Kodein.lazy {
        bind<EventBus>() with singleton { EventBus.getDefault() }
        import(createKodein())
    }

    open fun createKodein() = Kodein.Module {
        bind<AddressBook>() with singleton { FileBackedAddressBook() }
        bind<BalanceProvider>() with singleton { BalanceProvider() }
        bind<TransactionProvider>() with singleton { FileBackedTransactionProvider(instance()) }
        bind<ExchangeRateProvider>() with singleton { CachingExchangeProvider(FixedValueExchangeProvider(), instance()) }
        bind<SyncProgressProvider>() with singleton { SyncProgressProvider() }
    }

    private val keyStoreFile by lazy { File(filesDir, "keystore") }

    override fun onCreate() {
        super.onCreate()

        TraceDroid.init(this)
        AndroidThreeTen.init(this)

        keyStore = KeyStore(keyStoreFile.absolutePath, Geth.LightScryptN, Geth.LightScryptP)

        if (keyStore.accounts.size() > 0) {
            currentAddress = keyStore.accounts[0].address.toWallethAddress()
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        executeCodeWeWillIgnoreInTests()
    }

    open fun executeCodeWeWillIgnoreInTests() {
        startService(Intent(this, GethLightEthereumService::class.java))
    }

    companion object {
        lateinit var keyStore: KeyStore

        var currentAddress: WallethAddress? = null

        var networḱ: NetworkDefinition = RinkebyNetworkDefinition()
    }
}

