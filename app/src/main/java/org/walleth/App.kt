package org.walleth

import android.content.Context
import android.content.Intent
import android.os.StrictMode
import android.support.multidex.MultiDex
import android.support.multidex.MultiDexApplication
import android.support.v7.app.AppCompatDelegate
import com.chibatching.kotpref.Kotpref
import com.github.salomonbrys.kodein.*
import com.jakewharton.threetenabp.AndroidThreeTen
import okhttp3.OkHttpClient
import org.ligi.tracedroid.TraceDroid
import org.walleth.core.EtherScanService
import org.walleth.core.GethLightEthereumService
import org.walleth.core.GethTransactionSigner
import org.walleth.core.TransactionNotificationService
import org.walleth.data.BalanceProvider
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.addressbook.FileBackedAddressBook
import org.walleth.data.config.KotprefSettings
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.CryptoCompareExchangeProvider
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.exchangerate.TokenProvider
import org.walleth.data.keystore.GethBackedWallethKeyStore
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.tokens.FileBackedTokenProvider
import org.walleth.data.transactions.BaseTransactionProvider
import org.walleth.data.transactions.TransactionProvider

open class App : MultiDexApplication(), KodeinAware {

    override val kodein by Kodein.lazy {
        bind<OkHttpClient>() with singleton { OkHttpClient.Builder().build() }
        bind<NetworkDefinitionProvider>() with singleton { NetworkDefinitionProvider() }

        import(createKodein())
    }

    open fun createKodein() = Kodein.Module {
        bind<AddressBook>() with singleton { FileBackedAddressBook(this@App) }
        bind<BalanceProvider>() with singleton { BalanceProvider() }
        bind<TransactionProvider>() with singleton { BaseTransactionProvider() }
        bind<ExchangeRateProvider>() with singleton { CryptoCompareExchangeProvider(this@App, instance()) }
        bind<SyncProgressProvider>() with singleton { SyncProgressProvider() }
        bind<WallethKeyStore>() with singleton { GethBackedWallethKeyStore(this@App) }
        bind<Settings>() with singleton { KotprefSettings }
        bind<TokenProvider>() with singleton { FileBackedTokenProvider(this@App, instance()) }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build())
        }

        Kotpref.init(this)
        TraceDroid.init(this)
        AndroidThreeTen.init(this)

        applyNightMode(kodein.instance())
        executeCodeWeWillIgnoreInTests()
    }

    open fun executeCodeWeWillIgnoreInTests() {
        if (KotprefSettings.isLightClientWanted()) {
            startService(Intent(this, GethLightEthereumService::class.java))
        }
        startService(Intent(this, GethTransactionSigner::class.java))
        startService(Intent(this, EtherScanService::class.java))
        startService(Intent(this, TransactionNotificationService::class.java))
    }

    companion object {
        fun applyNightMode(settings: Settings) {
            @AppCompatDelegate.NightMode val nightMode = settings.getNightMode()
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }
}

