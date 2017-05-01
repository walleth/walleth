package org.ligi.walleth.activities

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View.INVISIBLE
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_in_drawer_container.*
import kotlinx.android.synthetic.main.value.*
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromClass
import org.ligi.tracedroid.TraceDroid
import org.ligi.tracedroid.sending.TraceDroidEmailSender
import org.ligi.walleth.R
import org.ligi.walleth.data.BalanceAtBlock
import org.ligi.walleth.data.BalanceProvider
import org.ligi.walleth.data.addressbook.AddressBook
import org.ligi.walleth.data.config.Settings
import org.ligi.walleth.data.exchangerate.ExchangeRateProvider
import org.ligi.walleth.data.keystore.WallethKeyStore
import org.ligi.walleth.data.syncprogress.SyncProgressProvider
import org.ligi.walleth.data.transactions.TransactionProvider
import org.ligi.walleth.functions.toEtherValueString
import org.ligi.walleth.iac.BarCodeIntentIntegrator
import org.ligi.walleth.iac.BarCodeIntentIntegrator.QR_CODE_TYPES
import org.ligi.walleth.iac.isERC67String
import org.ligi.walleth.ui.BaseTransactionRecyclerAdapter
import org.ligi.walleth.ui.ChangeObserver
import java.math.BigInteger


class MainActivity : AppCompatActivity() {

    val lazyKodein = LazyKodein(appKodein)

    val actionBarDrawerToggle by lazy { ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open, R.string.drawer_close) }

    val balanceProvider: BalanceProvider by lazyKodein.instance()
    val exchangeRateProvider: ExchangeRateProvider by lazyKodein.instance()
    val transactionProvider: TransactionProvider by lazyKodein.instance()
    val syncProgressProvider: SyncProgressProvider by lazyKodein.instance()
    val addressBook: AddressBook by lazyKodein.instance()
    val keyStore: WallethKeyStore by lazyKodein.instance()
    val settings: Settings by lazyKodein.instance()

    override fun onResume() {
        super.onResume()

        syncProgressProvider.registerChangeObserverWithInitialObservation(object : ChangeObserver {
            override fun observeChange() {
                runOnUiThread {
                    val progress = syncProgressProvider.currentSyncProgress

                    if (progress.isSyncing) {
                        val percent = ((progress.currentBlock.toDouble() / progress.highestBlock) * 100).toInt()
                        supportActionBar?.subtitle = "Block ${progress.currentBlock}/${progress.highestBlock} ($percent%)"
                    }
                }
            }
        })

        transactionProvider.registerChangeObserverWithInitialObservation(object : ChangeObserver {
            override fun observeChange() {
                val allTransactions = transactionProvider.getTransactionsForAddress(keyStore.getCurrentAddress())
                val incomingTransactions = allTransactions.filter { it.to == keyStore.getCurrentAddress()}.sortedByDescending { it.localTime }
                val outgoingTransactions = allTransactions.filter { it.from  == keyStore.getCurrentAddress() }.sortedByDescending { it.localTime }

                runOnUiThread {
                    transaction_recycler_out.adapter = BaseTransactionRecyclerAdapter(outgoingTransactions, addressBook)
                    transaction_recycler_in.adapter = BaseTransactionRecyclerAdapter(incomingTransactions, addressBook)
                }
            }
        })
        balanceProvider.registerChangeObserverWithInitialObservation(object : ChangeObserver {
            override fun observeChange() {
                var balanceForAddress = BalanceAtBlock(balance = BigInteger("0"), block = 0)
                balanceProvider.getBalanceForAddress(keyStore.getCurrentAddress())?.let {
                    balanceForAddress = it
                }
                val balanceIsZero = balanceForAddress.balance == BigInteger.ZERO

                runOnUiThread {
                    current_eth.text = balanceForAddress.balance.toEtherValueString()

                    val exChangeRate = exchangeRateProvider.getExchangeString(balanceForAddress.balance, settings.currentFiat)
                    current_fiat_symbol.text = settings.currentFiat
                    if (exChangeRate != null) {
                        current_fiat.text = exChangeRate
                    } else {
                        current_fiat.text = "?"
                    }

                    send_container.setVisibility(!balanceIsZero, INVISIBLE)
                    empty_view_container.setVisibility(balanceIsZero)

                    transaction_recycler_in.setVisibility(!balanceIsZero)
                    transaction_recycler_out.setVisibility(!balanceIsZero)

                    fab.setVisibility(!balanceIsZero)

                    if (!syncProgressProvider.currentSyncProgress.isSyncing) {
                        supportActionBar?.subtitle = "Block " + balanceForAddress.block
                    }
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && data.hasExtra("SCAN_RESULT")) {
            if (!data.getStringExtra("SCAN_RESULT").isERC67String()) {
                AlertDialog.Builder(this)
                        .setMessage("Only ERC67 supported currently")
                        .setPositiveButton("OK", null)
                        .show()
            } else {
                val intent = Intent(this, TransferActivity::class.java).apply {
                    setData(Uri.parse(data.getStringExtra("SCAN_RESULT")))
                }
                startActivity(intent)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (TraceDroid.getStackTraceFiles().isNotEmpty()) {
            TraceDroidEmailSender.sendStackTraces("ligi@ligi.de", this)
        }

        setContentView(R.layout.activity_main_in_drawer_container)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        drawer_layout.addDrawerListener(actionBarDrawerToggle)

        receive_container.setOnClickListener {
            startActivityFromClass(RequestActivity::class)
        }

        send_container.setOnClickListener {
            startActivityFromClass(TransferActivity::class)
        }

        fab.setOnClickListener {
            BarCodeIntentIntegrator(this).initiateScan(QR_CODE_TYPES)
        }

        transaction_recycler_out.layoutManager = LinearLayoutManager(this)
        transaction_recycler_in.layoutManager = LinearLayoutManager(this)

        current_fiat_symbol.setOnClickListener {
            startActivityFromClass(SelectReferenceActivity::class)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        actionBarDrawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        actionBarDrawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_info -> {
            startActivityFromClass(InfoActivity::class.java)
            true
        }
        else -> actionBarDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
    }

}
