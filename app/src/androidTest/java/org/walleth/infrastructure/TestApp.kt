package org.walleth.infrastructure

import android.arch.persistence.room.Room
import android.content.Context
import android.support.v7.app.AppCompatDelegate.MODE_NIGHT_YES
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.walleth.App
import org.walleth.data.AppDatabase
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.BaseCurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.syncprogress.WallethSyncProgress
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.transactions.TransactionProvider
import org.walleth.testdata.DefaultCurrentAddressProvider
import org.walleth.testdata.FixedValueExchangeProvider
import org.walleth.testdata.TestKeyStore
import org.walleth.testdata.TransactionProviderWithTestData

class TestApp : App() {

    override fun createKodein() = Kodein.Module {
        bind<TransactionProvider>() with singleton { transactionProvider }
        bind<ExchangeRateProvider>() with singleton { fixedValueExchangeProvider }
        bind<SyncProgressProvider>() with singleton {
            SyncProgressProvider().apply {
                setSyncProgress(WallethSyncProgress(true, 42000, 42042))
            }
        }
        bind<WallethKeyStore>() with singleton { keyStore }
        bind<Settings>() with singleton {

            mock(Settings::class.java).apply {
                `when`(currentFiat).thenReturn("EUR")
                `when`(getNightMode()).thenReturn(MODE_NIGHT_YES)
                `when`(startupWarningDone).thenReturn(true)
            }
        }
        bind<BaseCurrentAddressProvider>() with singleton { currentAddressProvider }
        bind<NetworkDefinitionProvider>() with singleton { networkDefinitionProvider }
        bind<CurrentTokenProvider>() with singleton { CurrentTokenProvider(instance()) }
        bind<AppDatabase>() with singleton {  testDatabase }
    }

    override fun executeCodeWeWillIgnoreInTests() = Unit
    override fun onCreate() {
        super.onCreate()
        resetDB(this)
    }

    companion object {
        val transactionProvider = TransactionProviderWithTestData()
        val fixedValueExchangeProvider = FixedValueExchangeProvider()
        val keyStore = TestKeyStore()
        val currentAddressProvider = DefaultCurrentAddressProvider()
        val networkDefinitionProvider = NetworkDefinitionProvider()

        lateinit var testDatabase: AppDatabase
        fun resetDB(context: Context) {
            testDatabase  = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        }

    }
}
