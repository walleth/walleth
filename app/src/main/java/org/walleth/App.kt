package org.walleth

import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.os.StrictMode
import androidx.annotation.XmlRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceScreen
import androidx.room.Room
import com.chibatching.kotpref.Kotpref
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.kethereum.keystore.api.InitializingFileKeyStore
import org.kethereum.keystore.api.KeyStore
import org.koin.android.ext.android.inject
import org.koin.android.ext.android.startKoin
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import org.ligi.tracedroid.TraceDroid
import org.walletconnect.impls.FileWCSessionStore
import org.walletconnect.impls.WCSessionStore
import org.walleth.activities.nfc.NFCCredentialStore
import org.walleth.contracts.FourByteDirectory
import org.walleth.contracts.FourByteDirectoryImpl
import org.walleth.core.TransactionNotificationService
import org.walleth.data.*
import org.walleth.data.addressbook.AccountKeySpec
import org.walleth.data.addressbook.allPrePopulationAddresses
import org.walleth.data.addressbook.toJSON
import org.walleth.data.blockexplorer.BlockExplorerProvider
import org.walleth.data.config.KotprefSettings
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.CryptoCompareExchangeProvider
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.InitializingCurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.getRootTokenForChain
import org.walleth.migrations.RecreatingMigration
import org.walleth.util.DelegatingSocketFactory
import org.walleth.viewmodels.TransactionListViewModel
import org.walleth.viewmodels.WalletConnectViewModel
import java.io.File
import java.net.Socket
import java.security.Security
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

    private val keyStore by lazy { InitializingFileKeyStore(File(filesDir, "keystore")) }
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
                    .addMigrations(RecreatingMigration(1, 4), RecreatingMigration(2, 4), RecreatingMigration(3, 4))
                    .build()
        }

        single { NetworkDefinitionProvider(get()) }
        single { BlockExplorerProvider(get()) }
        single {
            InitializingCurrentAddressProvider(settings = get()) as CurrentAddressProvider
        }
        single { FourByteDirectoryImpl(get(), applicationContext) as FourByteDirectory }

        single {
            FileWCSessionStore(File(this@App.filesDir, "walletConnectSessions.json").apply {
                createNewFile()
            }, get()) as WCSessionStore
        }

        single {
            NFCCredentialStore(this@App)
        }
        viewModel { TransactionListViewModel(this@App, get(), get(), get()) }
        viewModel { WalletConnectViewModel(this@App, get(), get()) }
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

        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())

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
        if (settings.addressInitVersion < 2) {
            settings.addressInitVersion = 2

            GlobalScope.launch(Dispatchers.Default) {
                appDatabase.addressBook.upsert(allPrePopulationAddresses)
            }
        }
        postInitCallbacks.forEach { it.invoke() }

        val currentTokenProvider: CurrentTokenProvider by inject()
        val networkDefinitionProvider: NetworkDefinitionProvider by inject()

        currentTokenProvider.setCurrent(getRootTokenForChain(networkDefinitionProvider.getCurrent()))

        if (settings.dataVersion < 1) {
            settings.dataVersion = 1
            GlobalScope.launch(Dispatchers.Default) {
                appDatabase.addressBook.all().forEach {
                    if (it.keySpec == null || it.keySpec?.isBlank() == true) {
                        val type = if (keyStore.hasKeyForForAddress(it.address)) ACCOUNT_TYPE_BURNER else ACCOUNT_TYPE_WATCH_ONLY
                        it.keySpec = AccountKeySpec(type).toJSON()
                        appDatabase.addressBook.upsert(it)
                    } else if (it.keySpec?.startsWith("m") == true) {
                        it.keySpec = AccountKeySpec(ACCOUNT_TYPE_TREZOR, derivationPath = it.keySpec).toJSON()
                        appDatabase.addressBook.upsert(it)
                    }
                }
            }
        }
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

