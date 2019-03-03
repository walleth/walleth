package org.walleth

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Room
import android.arch.persistence.room.migration.Migration
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
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.kethereum.keystore.api.InitializingKeyStore
import org.kethereum.keystore.api.KeyStore
import org.koin.android.ext.android.inject
import org.koin.android.ext.android.startKoin
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import org.ligi.tracedroid.TraceDroid
import org.walletconnect.impls.FileWCSessionStore
import org.walletconnect.impls.WCSessionStore
import org.walleth.contracts.FourByteDirectory
import org.walleth.contracts.FourByteDirectoryImpl
import org.walleth.core.TransactionNotificationService
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.addressbook.allPrePopulationAddresses
import org.walleth.data.blockexplorer.BlockExplorerProvider
import org.walleth.data.config.KotprefSettings
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.CryptoCompareExchangeProvider
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.initTokens
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.InitializingCurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.getRootTokenForChain
import org.walleth.util.DelegatingSocketFactory
import org.walleth.viewmodels.TransactionListViewModel
import org.walleth.viewmodels.WalletConnectViewModel
import java.io.File
import java.net.Socket
import javax.net.SocketFactory

open class App : MultiDexApplication() {

    private val koinModule = module {
        single { Moshi.Builder().build() }
        single {
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
    }

    private val keyStore by lazy { InitializingKeyStore(File(filesDir, "keystore")) }
    val appDatabase: AppDatabase by inject()
    val settings: Settings by inject()

    open fun createKoin() = module {

        single { CryptoCompareExchangeProvider(this@App, get()) as ExchangeRateProvider }
        single { SyncProgressProvider() }
        single { keyStore as KeyStore }
        single { KotprefSettings as Settings }
        single { CurrentTokenProvider(get()) }

        single {
            Room.databaseBuilder(applicationContext, AppDatabase::class.java, "maindb")
                    .addMigrations(object : Migration(1, 2) {
                        override fun migrate(database: SupportSQLiteDatabase) {
                            // we need to delete all transactions as the chainIDs have been changed
                            database.execSQL("DROP table `transactions`")

                            database.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`hash` TEXT NOT NULL, `extraIncomingAffectedAddress` TEXT, `chain` TEXT, `creationEpochSecond` INTEGER, `from` TEXT, `gasLimit` TEXT NOT NULL, `gasPrice` TEXT NOT NULL, `input` TEXT NOT NULL, `nonce` TEXT, `to` TEXT, `txHash` TEXT, `value` TEXT NOT NULL, `r` TEXT, `s` TEXT, `v` INTEGER, `needsSigningConfirmation` INTEGER NOT NULL, `source` TEXT NOT NULL, `relayed` TEXT NOT NULL, `eventLog` TEXT, `isPending` INTEGER NOT NULL, `error` TEXT, PRIMARY KEY(`hash`))")

                            // we need to delete all tokens as the root token name might have changed (before always ETH)
                            database.execSQL("DELETE FROM `tokens`")
                        }
                    })
                    .build()
        }

        single { NetworkDefinitionProvider(get()) }
        single { BlockExplorerProvider(get()) }
        single {
            InitializingCurrentAddressProvider(keyStore, get(), get(), applicationContext) as CurrentAddressProvider
        }
        single { FourByteDirectoryImpl(get(), applicationContext) as FourByteDirectory }

        single {
            FileWCSessionStore(File(this@App.filesDir, "walletConnectSessions.json").apply {
                createNewFile()
            }, get()) as WCSessionStore
        }

        viewModel { TransactionListViewModel(this@App, get(), get(), get()) }
        viewModel { WalletConnectViewModel(this@App, get(), get())  }
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

        startKoin(this, listOf(koinModule, createKoin()))

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build())
        }

        Kotpref.init(this)
        TraceDroid.init(this)
        AndroidThreeTen.init(this)
        applyNightMode(settings)
        executeCodeWeWillIgnoreInTests()
        initTokens(settings, assets, appDatabase)
        if (settings.addressInitVersion < 1) {
            settings.addressInitVersion = 1

            GlobalScope.launch(Dispatchers.Default) {
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

        val currentTokenProvider: CurrentTokenProvider by inject()
        val networkDefinitionProvider: NetworkDefinitionProvider by inject()

        currentTokenProvider.setCurrent(getRootTokenForChain(networkDefinitionProvider.getCurrent()))
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

