package org.walleth.activities

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Intent
import android.content.SharedPreferences
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
import org.kethereum.erc681.isEthereumURLString
import org.ligi.kaxt.recreateWhenPossible
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.qrscan.startScanActivityForResult
import org.walleth.data.AppDatabase
import org.walleth.data.balances.Balance
import org.walleth.data.config.Settings
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.transactions.TransactionEntity
import org.walleth.ui.TransactionAdapterDirection.INCOMING
import org.walleth.ui.TransactionAdapterDirection.OUTGOING
import org.walleth.ui.TransactionRecyclerAdapter
import java.math.BigInteger.ZERO


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val lazyKodein = LazyKodein(appKodein)

    private val actionBarDrawerToggle by lazy { ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open, R.string.drawer_close) }

    private val syncProgressProvider: SyncProgressProvider by lazyKodein.instance()
    private val networkDefinitionProvider: NetworkDefinitionProvider by lazyKodein.instance()
    private val appDatabase: AppDatabase by lazyKodein.instance()
    private val settings: Settings by lazyKodein.instance()
    private val currentTokenProvider: CurrentTokenProvider by lazyKodein.instance()
    private val currentAddressProvider: CurrentAddressProvider by lazyKodein.instance()
    private var lastNightMode: Int? = null
    private var balanceLiveData: LiveData<Balance>? = null
    private val onboardingController by lazy { OnboardingController(this, settings) }

    override fun onResume() {
        super.onResume()

        if (lastNightMode != null && lastNightMode != settings.getNightMode()) {
            recreateWhenPossible()
            return
        }
        lastNightMode = settings.getNightMode()
        setCurrentBalanceObserver()
    }

    private fun String.isJSONKey() = try {
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
                scanResult.isEthereumURLString() -> {
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

                scanResult.isUnsignedTransactionJSON() || scanResult.isSignedTransactionJSON() -> {
                    startActivity(getOfflineTransactionIntent(scanResult))
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

    fun refresh() {
        val incomingSize = transaction_recycler_in.adapter?.itemCount ?: 0
        val outgoingSize = transaction_recycler_out.adapter?.itemCount ?: 0

        val hasTransactions = incomingSize + outgoingSize > 0
        empty_view_container.setVisibility(!hasTransactions && !onboardingController.isShowing)
        transaction_recycler_out.setVisibility(hasTransactions)
        transaction_recycler_in.setVisibility(hasTransactions)
        send_container.setVisibility(hasTransactions, INVISIBLE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main_in_drawer_container)

        onboardingController.install()

        settings.registerListener(this)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        drawer_layout.addDrawerListener(actionBarDrawerToggle)

        receive_container.setOnClickListener {
            onboardingController.dismiss()
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


        val incomingTransactionsObserver = Observer<List<TransactionEntity>> {

            if (it != null) {
                transaction_recycler_in.adapter = TransactionRecyclerAdapter(it, appDatabase, INCOMING, networkDefinitionProvider)
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
                incomingTransactionsForAddress = appDatabase.transactions.getIncomingTransactionsForAddressOnChainOrdered(currentAddress, currentChain)
                outgoingTransactionsForAddress = appDatabase.transactions.getOutgoingTransactionsForAddressOnChainOrdered(currentAddress, currentChain)

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

        if (intent.action?.equals("org.walleth.action.SCAN") == true) {
            startScanActivityForResult(this)
        }

    }


    private val balanceObserver = Observer<Balance> {

        if (it != null) {
            value_view.setValue(it.balance, currentTokenProvider.currentToken)
            supportActionBar?.subtitle = getString(R.string.main_activity_block, it.block)
        } else {
            value_view.setValue(ZERO, currentTokenProvider.currentToken)
            supportActionBar?.subtitle = getString(R.string.main_activity_no_data)
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

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        if (Settings::currentFiat.name == key) {
            transaction_recycler_in.adapter?.notifyDataSetChanged()
            transaction_recycler_in.adapter?.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        settings.unregisterListener(this)
        super.onDestroy()
    }
}
