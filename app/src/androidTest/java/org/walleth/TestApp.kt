package org.walleth

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.singleton
import org.ligi.walleth.App
import org.ligi.walleth.data.BalanceProvider
import org.ligi.walleth.data.transactions.TransactionProvider
import org.ligi.walleth.data.addressbook.AddressBook
import org.ligi.walleth.data.config.Settings
import org.ligi.walleth.data.exchangerate.ExchangeRateProvider
import org.ligi.walleth.data.keystore.WallethKeyStore
import org.ligi.walleth.data.syncprogress.SyncProgressProvider
import org.ligi.walleth.data.syncprogress.WallethSyncProgress
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class TestApp : App() {

    override fun createKodein() = Kodein.Module {
        bind<AddressBook>() with singleton { addressBookWithEntries }
        bind<BalanceProvider>() with singleton { balanceProvider }
        bind<TransactionProvider>() with singleton { TransactionProviderWithTestData() }
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
            }
        }
    }

    override fun executeCodeWeWillIgnoreInTests() = Unit

    companion object {

        val fixedValueExchangeProvider = FixedValueExchangeProvider()
        val balanceProvider = BalanceProvider()
        val addressBookWithEntries = AddressBookWithTestEntries()
        val keyStore = TestKeyStore()
    }
}
