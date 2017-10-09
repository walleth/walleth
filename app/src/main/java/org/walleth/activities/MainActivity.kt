package org.walleth.activities

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
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
import org.json.JSONObject
import org.kethereum.erc67.isERC67String
import org.ligi.kaxt.recreateWhenPossible
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxtui.alert
import org.ligi.tracedroid.TraceDroid
import org.ligi.tracedroid.sending.TraceDroidEmailSender
import org.walleth.R
import org.walleth.activities.qrscan.startScanActivityForResult
import org.walleth.data.AppDatabase
import org.walleth.data.balances.Balance
import org.walleth.data.config.Settings
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.transactions.TransactionEntity
import org.walleth.ui.TransactionAdapterDirection.INCOMMING
import org.walleth.ui.TransactionAdapterDirection.OUTGOING
import org.walleth.ui.TransactionRecyclerAdapter
import java.math.BigInteger.ZERO


class MainActivity : AppCompatActivity() {

    val lazyKodein = LazyKodein(appKodein)

    val actionBarDrawerToggle by lazy { ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open, R.string.drawer_close) }

    val syncProgressProvider: SyncProgressProvider by lazyKodein.instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by lazyKodein.instance()
    val keyStore: WallethKeyStore by lazyKodein.instance()
    val appDatabase: AppDatabase by lazyKodein.instance()
    val settings: Settings by lazyKodein.instance()
    val currentTokenProvider: CurrentTokenProvider by lazyKodein.instance()
    val currentAddressProvider: CurrentAddressProvider by lazyKodein.instance()
    var lastNightMode: Int? = null

    var balanceLiveData: LiveData<Balance>? = null

    override fun onResume() {
        super.onResume()

        if (lastNightMode != null && lastNightMode != settings.getNightMode()) {
            recreateWhenPossible()
            return
        }
        lastNightMode = settings.getNightMode()
        setCurrentBalanceObserver()
    }

    fun String.isJSONKey() = try {
        JSONObject(this).let {
            it.has("address") && (it.has("crypto") || it.has("Crypto"))
        }
    } catch (e: Exception) {
        false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && data.hasExtra("SCAN_RESULT")) {
            val scanResult = data.getStringExtra("SCAN_RESULT")

            when {
                scanResult.isERC67String() -> {
                    startActivity(Intent(this, CreateTransactionActivity::class.java).apply {
                        setData(Uri.parse(scanResult))
                    })
                }

                scanResult.length == 64 -> {
                    startActivity(getKeyImportIntent(scanResult, KeyType.ECDSA))
                }

                scanResult.isJSONKey() -> {
                    startActivity(getKeyImportIntent(scanResult, KeyType.JSON))
                }

                scanResult.startsWith("0x") -> {
                    AlertDialog.Builder(this)
                            .setTitle(R.string.select_action_messagebox_title)
                            .setItems(R.array.scan_hex_choices, { _, which ->
                                when (which) {
                                    0 -> {
                                        startCreateAccountActivity(scanResult)
                                    }
                                    1 -> {
                                        startActivity(Intent(this, CreateTransactionActivity::class.java).apply {
                                            setData(Uri.parse("ethereum:$scanResult"))
                                        })
                                    }
                                    2 -> {
                                        alert("TODO")
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                }

                else -> {
                    AlertDialog.Builder(this)
                            .setMessage(R.string.scan_not_interpreted_error_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!settings.startupWarningDone) {
            alert(title = "Special Awareness", message = "Please note this is one alpha. Please do not work with important accounts and a lot of ether yet!")
            settings.startupWarningDone = true
        }

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
            startActivityFromClass(CreateTransactionActivity::class)
        }

        fab.setOnClickListener {
            startScanActivityForResult(this)
        }
        transaction_recycler_out.layoutManager = LinearLayoutManager(this)
        transaction_recycler_in.layoutManager = LinearLayoutManager(this)

        current_fiat_symbol.setOnClickListener {
            startActivityFromClass(SelectReferenceActivity::class)
        }

        current_token_symbol.setOnClickListener {
            startActivityFromClass(SelectTokenActivity::class)
        }


        syncProgressProvider.observe(this, Observer {
            val progress = it!!

            if (progress.isSyncing) {
                val percent = ((progress.currentBlock.toDouble() / progress.highestBlock) * 100).toInt()
                supportActionBar?.subtitle = "Block ${progress.currentBlock}/${progress.highestBlock} ($percent%)"
            }
        })

        fun refresh() {
            val incommingSize = transaction_recycler_in.adapter?.itemCount ?: 0
            val outgoingSize = transaction_recycler_out.adapter?.itemCount ?: 0

            val hasTransactions = incommingSize + outgoingSize > 0
            empty_view_container.setVisibility(!hasTransactions)
            transaction_recycler_out.setVisibility(hasTransactions)
            transaction_recycler_in.setVisibility(hasTransactions)
            send_container.setVisibility(hasTransactions, INVISIBLE)
        }

        val incomingTransactionsObserver = Observer<List<TransactionEntity>> {

            if (it != null) {
                transaction_recycler_in.adapter = TransactionRecyclerAdapter(it, appDatabase, INCOMMING, networkDefinitionProvider)
                transaction_recycler_in.setVisibility(!it.isEmpty())
                refresh()
            }
        }

        val outgoingTransactionsObserver = Observer<List<TransactionEntity>> {

            if (it != null) {
                transaction_recycler_out.adapter = TransactionRecyclerAdapter(it, appDatabase, OUTGOING, networkDefinitionProvider)
                refresh()
            }
        }

        var incomingTransactionsForAddress: LiveData<List<TransactionEntity>>? = null
        var outgoingTransactionsForAddress: LiveData<List<TransactionEntity>>? = null

        fun installTransactionObservers() {

            incomingTransactionsForAddress?.removeObserver(incomingTransactionsObserver)
            outgoingTransactionsForAddress?.removeObserver(outgoingTransactionsObserver)

            currentAddressProvider.value?.let { currentAddress ->
                val currentChain = networkDefinitionProvider.getCurrent().chain
                incomingTransactionsForAddress = appDatabase.transactions.getIncomingTransactionsForAddress(currentAddress, currentChain)
                outgoingTransactionsForAddress = appDatabase.transactions.getOutgoingTransactionsForAddress(currentAddress, currentChain)

                incomingTransactionsForAddress?.observe(this, incomingTransactionsObserver)
                outgoingTransactionsForAddress?.observe(this, outgoingTransactionsObserver)
            }
        }

        networkDefinitionProvider.observe(this, Observer {
            setCurrentBalanceObserver()
            installTransactionObservers()
        })

        currentAddressProvider.observe(this, Observer {
            installTransactionObservers()
        })

        currentAddressProvider.observe(this, Observer { _ ->
            setCurrentBalanceObserver()
        })

    }


    private val balanceObserver = Observer<Balance> {

        if (it!=null) {
            value_view.setValue(it.balance, currentTokenProvider.currentToken)
            supportActionBar?.subtitle = "Block " + it.block
        } else {
            value_view.setValue(ZERO, currentTokenProvider.currentToken)
            supportActionBar?.subtitle = "No data"
        }
    }

    private fun setCurrentBalanceObserver() {
        val currentAddress = currentAddressProvider.value
        if (currentAddress != null) {
            balanceLiveData?.removeObserver(balanceObserver)
            balanceLiveData = appDatabase.balances.getBalanceLive(currentAddress, currentTokenProvider.currentToken.address, networkDefinitionProvider.getCurrent().chain)
            balanceLiveData?.observe(this, balanceObserver)
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
