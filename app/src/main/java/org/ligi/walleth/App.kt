package org.ligi.walleth

import android.app.Application
import android.content.Intent
import android.support.v7.app.AppCompatDelegate
import com.github.salomonbrys.kodein.*
import com.jakewharton.threetenabp.AndroidThreeTen
import okhttp3.OkHttpClient
import org.ligi.tracedroid.TraceDroid
import org.ligi.walleth.core.GethLightEthereumService
import org.ligi.walleth.data.BalanceProvider
import org.ligi.walleth.data.FileBackedTransactionProvider
import org.ligi.walleth.data.TransactionProvider
import org.ligi.walleth.data.addressbook.AddressBook
import org.ligi.walleth.data.addressbook.FileBackedAddressBook
import org.ligi.walleth.data.exchangerate.CryptoCompareExchangeProvider
import org.ligi.walleth.data.exchangerate.ExchangeRateProvider
import org.ligi.walleth.data.keystore.GethBackedWallethKeyStore
import org.ligi.walleth.data.keystore.WallethKeyStore
import org.ligi.walleth.data.networks.NetworkDefinition
import org.ligi.walleth.data.networks.RinkebyNetworkDefinition
import org.ligi.walleth.data.syncprogress.SyncProgressProvider

open class App : Application(), KodeinAware {

    override val kodein by Kodein.lazy {
        import(createKodein())
    }

    open fun createKodein() = Kodein.Module {
        bind<AddressBook>() with singleton { FileBackedAddressBook() }
        bind<BalanceProvider>() with singleton { BalanceProvider() }
        bind<OkHttpClient>() with singleton { OkHttpClient.Builder().build() }
        bind<TransactionProvider>() with singleton { FileBackedTransactionProvider() }
        bind<ExchangeRateProvider>() with singleton { CryptoCompareExchangeProvider(this@App, instance()) }
        bind<SyncProgressProvider>() with singleton { SyncProgressProvider() }
        bind<WallethKeyStore>() with singleton { GethBackedWallethKeyStore(this@App) }
    }

    override fun onCreate() {
        super.onCreate()

        TraceDroid.init(this)
        AndroidThreeTen.init(this)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        executeCodeWeWillIgnoreInTests()
    }

    open fun executeCodeWeWillIgnoreInTests() {
        startService(Intent(this, GethLightEthereumService::class.java))
    }

    companion object {
        var networḱ: NetworkDefinition = RinkebyNetworkDefinition()
    }
}

