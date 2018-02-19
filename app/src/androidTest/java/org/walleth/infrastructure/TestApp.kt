package org.walleth.infrastructure

import android.arch.persistence.room.Room
import android.content.Context
import android.support.v7.app.AppCompatDelegate.MODE_NIGHT_YES
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.walleth.App
import org.walleth.contracts.FourByteDirectory
import org.walleth.data.AppDatabase
import org.walleth.data.DEFAULT_GAS_PRICE
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.networks.RINKEBY_CHAIN_ID
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.syncprogress.WallethSyncProgress
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.kethereum.model.ContractFunction
import org.walleth.testdata.DefaultCurrentAddressProvider
import org.walleth.testdata.FixedValueExchangeProvider
import org.walleth.testdata.TestKeyStore

private fun <T> any(): T {
    Mockito.any<T>()
    return uninitialized()
}

private fun <T> uninitialized(): T = null as T

class TestApp : App() {

    override fun createKodein() = Kodein.Module {
        bind<ExchangeRateProvider>() with singleton { fixedValueExchangeProvider }
        bind<SyncProgressProvider>() with singleton {
            SyncProgressProvider().apply {
                value = WallethSyncProgress(true, 42000, 42042)
            }
        }
        bind<WallethKeyStore>() with singleton { keyStore }
        bind<Settings>() with singleton { mySettings }
        bind<CurrentAddressProvider>() with singleton { currentAddressProvider }
        bind<NetworkDefinitionProvider>() with singleton { networkDefinitionProvider }
        bind<CurrentTokenProvider>() with singleton { currentTokenProvider }
        bind<AppDatabase>() with singleton { testDatabase }
        bind<FourByteDirectory>() with singleton { testFourByteDirectory }
    }

    override fun executeCodeWeWillIgnoreInTests() = Unit
    override fun onCreate() {
        resetDB(this)
        super.onCreate()
    }

    companion object {
        val fixedValueExchangeProvider = FixedValueExchangeProvider()
        val keyStore = TestKeyStore()
        val mySettings = mock(Settings::class.java).apply {
            `when`(currentFiat).thenReturn("EUR")
            `when`(getNightMode()).thenReturn(MODE_NIGHT_YES)
            `when`(startupWarningDone).thenReturn(true)
            `when`(chain).thenReturn(RINKEBY_CHAIN_ID)
            `when`(isLightClientWanted()).thenReturn(false)
            `when`(addressInitVersion).thenReturn(0)
            `when`(tokensInitVersion).thenReturn(0)
            `when`(getGasPriceFor(any())).thenReturn(DEFAULT_GAS_PRICE)
        }
        val currentAddressProvider = DefaultCurrentAddressProvider(mySettings, keyStore)
        val networkDefinitionProvider = NetworkDefinitionProvider(mySettings)
        val currentTokenProvider = CurrentTokenProvider(networkDefinitionProvider)

        val contractFunctionTextSignature1 = "aFunctionCall1(address)"
        val contractFunctionTextSignature2 = "aFunctionCall2(address)"
        val testFourByteDirectory = mock(FourByteDirectory::class.java).apply {
            `when`(getSignaturesFor(any())).then { invocation ->
                listOf(
                        ContractFunction(invocation.arguments[0] as String, textSignature = contractFunctionTextSignature1),
                        ContractFunction(invocation.arguments[0] as String, textSignature = contractFunctionTextSignature2)
                )
            }
        }
        lateinit var testDatabase: AppDatabase
        fun resetDB(context: Context) {
            testDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        }

    }
}
