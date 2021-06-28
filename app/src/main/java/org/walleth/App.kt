package org.walleth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.XmlRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceScreen
import androidx.room.Room
import com.chibatching.kotpref.Kotpref
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.kethereum.keystore.api.InitializingFileKeyStore
import org.kethereum.keystore.api.KeyStore
import org.kethereum.metadata.repo.MetaDataRepoHttpWithCacheImpl
import org.kethereum.metadata.repo.model.MetaDataRepo
import org.kethereum.methodsignatures.CachedOnlineMethodSignatureRepository
import org.kethereum.methodsignatures.CachedOnlineMethodSignatureRepositoryImpl
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.ligi.tracedroid.TraceDroid
import org.walletconnect.impls.FileWCSessionStore
import org.walletconnect.impls.WCSessionStore
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.*
import org.walleth.data.addresses.*
import org.walleth.data.blockexplorer.BlockExplorerProvider
import org.walleth.data.config.KotprefSettings
import org.walleth.data.config.Settings
import org.walleth.data.ens.ENSProvider
import org.walleth.data.ens.ENSProviderImpl
import org.walleth.data.exchangerate.CryptoCompareExchangeProvider
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.rpc.RPCProvider
import org.walleth.data.rpc.RPCProviderImpl
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.CurrentTokenProviderImpl
import org.walleth.migrations.ChainAddingAndRecreatingMigration
import org.walleth.migrations.EIP1559Migration
import org.walleth.migrations.TransactionExtendingMigration
import org.walleth.nfc.NFCCredentialStore
import org.walleth.notifications.TransactionNotificationService
import org.walleth.overview.TransactionListViewModel
import org.walleth.startup.StartupViewModel
import org.walleth.util.enableStrictMode
import org.walleth.util.jsonadapter.BigIntegerJSONAdapter
import org.walleth.walletconnect.WalletConnectViewModel
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import java.security.Security


open class App : MultiDexApplication() {

    private val koinModule = module {
        single { Moshi.Builder().add(BigIntegerJSONAdapter()).build() }
    }

    private val keyStore by lazy { InitializingFileKeyStore(File(filesDir, "keystore")) }
    val appDatabase: AppDatabase by inject()
    val settings: Settings by inject()

    open fun createKoin() = module {
        single<ExchangeRateProvider> { CryptoCompareExchangeProvider(this@App, get()) }
        single { SyncProgressProvider() }
        single<KeyStore> { keyStore }
        single<Settings> { KotprefSettings }
        single<CurrentTokenProvider> { CurrentTokenProviderImpl(get()) }
        single<RPCProvider> { RPCProviderImpl(network = get(), appDatabase = get(), okHttpClient = get(), settings = get()) }
        single<ENSProvider> { ENSProviderImpl(get()) }
        single {
            Room.databaseBuilder(applicationContext, AppDatabase::class.java, "maindb")
                    .addMigrations(
                            ChainAddingAndRecreatingMigration(1),
                            ChainAddingAndRecreatingMigration(2),
                            ChainAddingAndRecreatingMigration(3),
                            ChainAddingAndRecreatingMigration(4),
                            TransactionExtendingMigration(),
                            EIP1559Migration()
                    ).build()
        }

        single { ChainInfoProvider(settings = get(), appDatabase = get(), keyStore = get(), moshi = get(), assetManager = assets) }
        single { BlockExplorerProvider(get()) }
        single<CurrentAddressProvider> {
            CurrentAddressProvider(settings = get())
        }
        single<CachedOnlineMethodSignatureRepository> {
            CachedOnlineMethodSignatureRepositoryImpl(get(), File(cacheDir, "funsignatures").apply {
                mkdirs()
            })
        }

        single<WCSessionStore> {
            FileWCSessionStore(File(this@App.filesDir, "walletConnectSessions.json").apply {
                createNewFile()
            }, get())
        }

        single {
            NFCCredentialStore(this@App)
        }

        single<MetaDataRepo> {
            MetaDataRepoHttpWithCacheImpl(cacheDir = File(cacheDir, "metadata").apply { mkdirs() })
        }

        single {
            OkHttpClient.Builder().build()
        }
        viewModel { TransactionListViewModel(this@App, get(), get(), get()) }
        viewModel { WalletConnectViewModel(this@App, get(), get(), get()) }
        viewModel { StartupViewModel(get(), get()) }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(listOf(koinModule, createKoin()))
        }

        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 28) {
            enableStrictMode()
        }

        Kotpref.init(this)
        TraceDroid.init(this)
        Timber.plant(DebugTree())
        AndroidThreeTen.init(this)
        applyNightMode(settings)
        executeCodeWeWillIgnoreInTests()
        if (settings.addressInitVersion < 2) {
            settings.addressInitVersion = 2

            GlobalScope.launch(Dispatchers.Default) {
                appDatabase.addressBook.upsert(allPrePopulationAddresses)
            }
        }
        postInitCallbacks.forEach { it.invoke() }

        val currentTokenProvider: CurrentTokenProvider by inject()
        val chainInfoProvider: ChainInfoProvider by inject()


    }

    open fun executeCodeWeWillIgnoreInTests() {
        try {
            startService(Intent(this, TransactionNotificationService::class.java))
        } catch (e: IllegalStateException) {
        }
    }

    companion object {
        val activeActivities: MutableSet<Activity> = HashSet()
        val visibleActivities: MutableSet<Activity> = HashSet()

        // A process lifecycle observer would be nicer here - but ran into serious trouble here - so doing this for now
        val onActivityToForegroundObserver: MutableSet<() -> Unit> = HashSet()

        val postInitCallbacks = mutableListOf<() -> Unit>()
        val extraPreferences = mutableListOf<Pair<@XmlRes Int, (preferenceScreen: PreferenceScreen) -> Unit>>()

        fun applyNightMode(settings: Settings) {
            @AppCompatDelegate.NightMode val nightMode = settings.getNightMode()
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }
}

