package org.walleth

import android.arch.persistence.room.Room
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.os.StrictMode
import android.support.annotation.XmlRes
import android.support.multidex.MultiDex
import android.support.multidex.MultiDexApplication
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.PreferenceScreen
import com.chibatching.kotpref.Kotpref
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import okhttp3.OkHttpClient
import org.kethereum.crypto.initializeCrypto
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.ligi.tracedroid.TraceDroid
import org.walleth.contracts.FourByteDirectory
import org.walleth.contracts.FourByteDirectoryImpl
import org.walleth.core.TransactionNotificationService
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.addressbook.allPrePopulationAddresses
import org.walleth.data.config.KotprefSettings
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.CryptoCompareExchangeProvider
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.initTokens
import org.walleth.data.keystore.FileBasedWallethKeyStore
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.InitializingCurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.util.DelegatingSocketFactory
import java.net.Socket
import javax.net.SocketFactory

open class App : MultiDexApplication(), KodeinAware {

    override val kodein = Kodein.lazy {
        bind<OkHttpClient>() with singleton {
            val socketFactory = object : DelegatingSocketFactory(SocketFactory.getDefault()) {
                override fun configureSocket(socket: Socket): Socket {
                    // https://github.com/walleth/walleth/issues/164
                    // https://github.com/square/okhttp/issues/3537
                    TrafficStats.tagSocket(socket)

                    return socket
                }
            }
            OkHttpClient.Builder().socketFactory(socketFactory).build()
        }

        import(createKodein())
    }

    private val keyStore by lazy { FileBasedWallethKeyStore(this) }
    val appDatabase: AppDatabase by instance()
    val settings: Settings by instance()

    open fun createKodein(): Kodein.Module {

        return Kodein.Module {
            bind<ExchangeRateProvider>() with singleton { CryptoCompareExchangeProvider(this@App, instance()) }
            bind<SyncProgressProvider>() with singleton { SyncProgressProvider() }
            bind<WallethKeyStore>() with singleton { keyStore }
            bind<Settings>() with singleton { KotprefSettings }

            bind<CurrentTokenProvider>() with singleton { CurrentTokenProvider(instance()) }

            bind<AppDatabase>() with singleton { Room.databaseBuilder(applicationContext, AppDatabase::class.java, "maindb").build() }
            bind<NetworkDefinitionProvider>() with singleton { NetworkDefinitionProvider(instance()) }
            bind<CurrentAddressProvider>() with singleton { InitializingCurrentAddressProvider(keyStore, instance(), instance(), applicationContext) }
            bind<FourByteDirectory>() with singleton { FourByteDirectoryImpl(instance(), applicationContext) }
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)
        // Normal app init code...

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build())
        }

        Kotpref.init(this)
        TraceDroid.init(this)
        initializeCrypto()
        AndroidThreeTen.init(this)
        applyNightMode(settings)
        executeCodeWeWillIgnoreInTests()
        initTokens(settings, assets, appDatabase)
        if (settings.addressInitVersion < 1) {
            settings.addressInitVersion = 1

            async(CommonPool) {
                keyStore.getAddresses().forEachIndexed { index, address ->

                    appDatabase.addressBook.upsert(AddressBookEntry(
                            name = "Default" + if (keyStore.getAddresses().size > 1) index else "",
                            address = address,
                            note = "default account with key",
                            isNotificationWanted = false,
                            trezorDerivationPath = null
                    ))
                }
                appDatabase.addressBook.upsert(allPrePopulationAddresses)
            }
        }
        postInitCallbacks.forEach { it.invoke() }
    }

    open fun executeCodeWeWillIgnoreInTests() {
        try {
            startService(Intent(this, TransactionNotificationService::class.java))
        } catch (e: IllegalStateException) {
        }
    }

    companion object {
        val postInitCallbacks = mutableListOf<() -> Unit>()
        val extraPreferences = mutableListOf<Pair<@XmlRes Int, (preferenceScreen: PreferenceScreen) -> Unit>>()

        fun applyNightMode(settings: Settings) {
            @AppCompatDelegate.NightMode val nightMode = settings.getNightMode()
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }
}

