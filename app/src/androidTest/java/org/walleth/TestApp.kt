package org.walleth

import android.support.v7.app.AppCompatDelegate.MODE_NIGHT_YES
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.singleton
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.walleth.data.BalanceProvider
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.syncprogress.WallethSyncProgress
import org.walleth.data.transactions.TransactionProvider

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
                `when`(getNightMode()).thenReturn(MODE_NIGHT_YES)
                `when`(startupWarningDone).thenReturn(true)
            }
        }
    }

    override fun executeCodeWeWillIgnoreInTests() = Unit

    companion object {

        val fixedValueExchangeProvider = FixedValueExchangeProvider()
        val balanceProvider = BalanceProviderWithResetFun()
        val addressBookWithEntries = AddressBookWithTestEntries()
        val keyStore = TestKeyStore()
    }
}
