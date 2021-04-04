package org.walleth.infrastructure

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.room.Room
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.kethereum.DEFAULT_GAS_PRICE
import org.kethereum.keystore.api.KeyStore
import org.kethereum.metadata.repo.model.MetaDataRepo
import org.kethereum.methodsignatures.CachedOnlineMethodSignatureRepository
import org.kethereum.methodsignatures.model.TextMethodSignature
import org.kethereum.rpc.EthereumRPC
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.walletconnect.impls.WCSessionStore
import org.walleth.App
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.AppDatabase
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.rpc.DescribedRPC
import org.walleth.data.rpc.RPCProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.syncprogress.WallethSyncProgress
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.CurrentTokenProviderImpl
import org.walleth.overview.TransactionListViewModel
import org.walleth.testdata.DefaultCurrentAddressProvider
import org.walleth.testdata.FixedValueExchangeProvider
import org.walleth.testdata.TestKeyStore
import org.walleth.util.jsonadapter.BigIntegerJSONAdapter
import org.walleth.walletconnect.WalletConnectViewModel
import java.math.BigInteger.ZERO

private fun <T> any(): T {
    Mockito.any<T>()
    return uninitialized()
}

private fun <T> uninitialized(): T = null as T

class TestApp : App() {

    override fun createKoin() = module {
        single<ExchangeRateProvider> { fixedValueExchangeProvider }
        single {
            SyncProgressProvider().apply {
                value = WallethSyncProgress(true, 42000, 42042)
            }
        }
        single<KeyStore> { keyStore }
        single { mySettings }
        single<CurrentAddressProvider> { currentAddressProvider }
        single { chainInfoProvider }
        single<CurrentTokenProvider> { currentTokenProvider }
        single { testDatabase }
        single { testFourByteDirectory }
        single {
            mock(RPCProvider::class.java).apply {
                runBlocking {
                    `when`(get()).thenReturn(RPCMock)
                }
            }
        }

        single {
            mock(WCSessionStore::class.java)
        }

        single {
            mock(OkHttpClient::class.java)
        }

        single {
            mock(MetaDataRepo::class.java)
        }

        viewModel { WalletConnectViewModel(this@TestApp, get(), get(), get()) }
        viewModel { TransactionListViewModel(this@TestApp, get(), get(), get()) }
    }

    override fun executeCodeWeWillIgnoreInTests() = Unit
    override fun onCreate() {
        companionContext = this
        resetDB()
        super.onCreate()
    }

    companion object {
        val RPCMock: DescribedRPC = mock(DescribedRPC::class.java).apply {
            `when`(estimateGas(any())).thenReturn(ZERO)
        }
        val fixedValueExchangeProvider = FixedValueExchangeProvider()
        val keyStore = TestKeyStore()
        val mySettings: Settings = mock(Settings::class.java).apply {
            `when`(currentFiat).thenReturn("EUR")
            `when`(getNightMode()).thenReturn(MODE_NIGHT_YES)
            `when`(onboardingDone).thenReturn(true)
            `when`(chain).thenReturn(4L)
            `when`(isLightClientWanted()).thenReturn(false)
            `when`(addressInitVersion).thenReturn(0)
            `when`(tokensInitVersion).thenReturn(0)
            `when`(getGasPriceFor(any())).thenReturn(DEFAULT_GAS_PRICE)
        }
        val currentAddressProvider = DefaultCurrentAddressProvider(mySettings, keyStore)
        val chainInfoProvider by lazy {
            ChainInfoProvider(mySettings, testDatabase, keyStore, Moshi.Builder().add(BigIntegerJSONAdapter()).build(), companionContext!!.assets)
        }
        val currentTokenProvider by lazy {
            CurrentTokenProviderImpl(chainInfoProvider)
        }

        const val contractFunctionTextSignature1 = "aFunctionCall1(address)"
        const val contractFunctionTextSignature2 = "aFunctionCall2(address)"
        val testFourByteDirectory: CachedOnlineMethodSignatureRepository = mock(CachedOnlineMethodSignatureRepository::class.java).apply {
            `when`(getSignaturesFor(any())).then {
                listOf(
                        TextMethodSignature(contractFunctionTextSignature1),
                        TextMethodSignature(contractFunctionTextSignature2)
                )
            }
        }

        val testDatabase by lazy {
            Room.inMemoryDatabaseBuilder(companionContext!!, AppDatabase::class.java).build()
        }
        var companionContext: Context? = null
        fun resetDB() {
            GlobalScope.launch(Dispatchers.Default) {
                testDatabase.clearAllTables()
            }
        }

    }
}
