package org.walleth

import android.arch.persistence.room.Room
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.StrictMode
import android.support.multidex.MultiDex
import android.support.multidex.MultiDexApplication
import android.support.v7.app.AppCompatDelegate
import com.chibatching.kotpref.Kotpref
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.appKodein
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import okhttp3.OkHttpClient
import org.kethereum.model.Address
import org.ligi.tracedroid.TraceDroid
import org.walleth.core.EtherScanService
import org.walleth.core.GethLightEthereumService
import org.walleth.core.GethTransactionSigner
import org.walleth.core.TransactionNotificationService
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.config.KotprefSettings
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.CryptoCompareExchangeProvider
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.initTokens
import org.walleth.data.keystore.GethBackedWallethKeyStore
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.InitializingCurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.tokens.CurrentTokenProvider

open class App : MultiDexApplication(), KodeinAware {

    override val kodein by Kodein.lazy {
        bind<OkHttpClient>() with singleton { OkHttpClient.Builder().build() }

        import(createKodein())
    }

    private val gethBackedWallethKeyStore by lazy { GethBackedWallethKeyStore(this) }
    val appDatabase: AppDatabase by LazyKodein(appKodein).instance()
    val settings: Settings by LazyKodein(appKodein).instance()

    open fun createKodein(): Kodein.Module {

        return Kodein.Module {
            bind<ExchangeRateProvider>() with singleton { CryptoCompareExchangeProvider(this@App, instance()) }
            bind<SyncProgressProvider>() with singleton { SyncProgressProvider() }
            bind<WallethKeyStore>() with singleton { gethBackedWallethKeyStore }
            bind<Settings>() with singleton { KotprefSettings }

            bind<CurrentTokenProvider>() with singleton { CurrentTokenProvider(instance()) }

            bind<AppDatabase>() with singleton { Room.databaseBuilder(applicationContext, AppDatabase::class.java, "maindb").build() }
            bind<NetworkDefinitionProvider>() with singleton { NetworkDefinitionProvider(instance()) }
            bind<CurrentAddressProvider>() with singleton { InitializingCurrentAddressProvider(gethBackedWallethKeyStore, instance(), instance(),applicationContext) }
        }
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
                    .build())
        }

        Kotpref.init(this)
        TraceDroid.init(this)
        AndroidThreeTen.init(this)

        applyNightMode(kodein.instance())
        executeCodeWeWillIgnoreInTests()
        initTokens(settings, assets, appDatabase)
        if (settings.addressInitVersion < 1) {
            settings.addressInitVersion = 1

            async(CommonPool) {
                val keyCount = gethBackedWallethKeyStore.keyStore.accounts.size()
                (0 until keyCount).forEach {
                    val account = gethBackedWallethKeyStore.keyStore.accounts.get(it)
                    appDatabase.addressBook.upsert(AddressBookEntry(
                            name = "Default" + if (keyCount > 1) it else "",
                            address = Address(account.address.hex),
                            note = "default account with key",
                            isNotificationWanted = false,
                            trezorDerivationPath = null
                    ))
                }
                appDatabase.addressBook.upsert(listOf(AddressBookEntry(
                        name = "Michael Cook",
                        address = Address("0xbE27686a93c54Af2f55f16e8dE9E6Dc5dccE915e"),
                        note = "Icon designer - please tip him well if you want things to look nice",
                        isNotificationWanted = false,
                        trezorDerivationPath = null
                ), AddressBookEntry(
                        name = "LIGI",
                        address = Address("0x381e247bef0ebc21b6611786c665dd5514dcc31f"),
                        note = "Developer & Ideator - send some ETH if you like this project and want it to continue",
                        isNotificationWanted = false,
                        trezorDerivationPath = null

                ), AddressBookEntry(
                        name = "Faucet",
                        address = Address("0x31b98d14007bdee637298086988a0bbd31184523"),
                        note = "The source of some rinkeby ether",
                        isNotificationWanted = false,
                        trezorDerivationPath = null
                )))
            }
        }
    }

    open fun executeCodeWeWillIgnoreInTests() {
        if (settings.isLightClientWanted()) {
            Handler().postDelayed({
                startService(Intent(this, GethLightEthereumService::class.java))
            }, 2000)

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

