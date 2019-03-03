package org.walleth.activities

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View.INVISIBLE
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_in_drawer_container.*
import kotlinx.android.synthetic.main.toolbar.*
import org.json.JSONObject
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.erc681.toERC681
import org.kethereum.erc831.isEthereumURLString
import org.kethereum.model.EthereumURI
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import org.ligi.kaxt.livedata.nonNull
import org.ligi.kaxt.livedata.observe
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.qrscan.startScanActivityForResult
import org.walleth.activities.walletconnect.getWalletConnectIntent
import org.walleth.data.AppDatabase
import org.walleth.data.balances.Balance
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.getRootTokenForChain
import org.walleth.ui.TransactionAdapterDirection.INCOMING
import org.walleth.ui.TransactionAdapterDirection.OUTGOING
import org.walleth.ui.TransactionRecyclerAdapter
import org.walleth.ui.valueview.ValueViewController
import org.walleth.util.copyToClipboard
import org.walleth.util.isParityUnsignedTransactionJSON
import org.walleth.util.isSignedTransactionJSON
import org.walleth.util.isUnsignedTransactionJSON
import org.walleth.viewmodels.TransactionListViewModel
import java.math.BigInteger.ZERO

private const val KEY_LAST_PASTED_DATA: String = "LAST_PASTED_DATA"

class MainActivity : WallethActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val actionBarDrawerToggle by lazy { ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open, R.string.drawer_close) }

    private val syncProgressProvider: SyncProgressProvider by inject()
    private val networkDefinitionProvider: NetworkDefinitionProvider by inject()
    private val appDatabase: AppDatabase by inject()
    private val settings: Settings by inject()
    private val currentTokenProvider: CurrentTokenProvider by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val exchangeRateProvider: ExchangeRateProvider by inject()

    private val transactionViewModel: TransactionListViewModel by viewModel()

    private var lastNightMode: Int? = null
    private var balanceLiveData: LiveData<Balance>? = null
    private var etherLiveData: LiveData<Balance>? = null
    private val onboardingController by lazy { OnboardingController(this, transactionViewModel, settings) }

    private var lastPastedData: String? = null

    private val amountViewModel by lazy {
        ValueViewController(value_view, exchangeRateProvider, settings)
    }

    override fun onResume() {
        super.onResume()

        if (lastNightMode != null && lastNightMode != settings.getNightMode()) {
            recreate()
            return
        }
        lastNightMode = settings.getNightMode()
        setCurrentBalanceObservers()

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val item = clipboard.primaryClip?.let { it.getItemAt(0).text?.toString() }
            val erc681 = item?.let { EthereumURI(it).toERC681() }
            if (erc681?.valid == true && erc681.address != null && item != lastPastedData && item != currentAddressProvider.value?.hex.let { ERC681(address = it).generateURL() }) {
                lastPastedData = item
                Snackbar.make(fab, R.string.paste_from_clipboard, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.paste_from_clipboard_action) {
                            alert(R.string.copied_string_warning_message, R.string.copied_string_warning_title) {
                                startActivity(Intent(this@MainActivity, CreateTransactionActivity::class.java).apply {
                                    data = Uri.parse(item)
                                })
                            }

                        }
                        .show()
            }
        }
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
                scanResult.startsWith("wc:") -> {
                    startActivity(getWalletConnectIntent(Uri.parse(scanResult)))
                }

                scanResult.isEthereumURLString() -> {
                    startActivity(getEthereumViewIntent(scanResult))
                }

                scanResult.length == 64 -> {
                    startActivity(getKeyImportIntent(scanResult, KeyType.ECDSA))
                }

                scanResult.isJSONKey() -> {
                    startActivity(getKeyImportIntent(scanResult, KeyType.JSON))
                }

                scanResult.isUnsignedTransactionJSON() || scanResult.isSignedTransactionJSON() || scanResult.isParityUnsignedTransactionJSON() -> {
                    startActivity(getOfflineTransactionIntent(scanResult))
                }

                scanResult.startsWith("0x") -> {
                    startActivity(getEthereumViewIntent(ERC681(address = scanResult).generateURL()))
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

        setContentView(R.layout.activity_main_in_drawer_container)

        onboardingController.install()

        settings.registerListener(this)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        drawer_layout.addDrawerListener(actionBarDrawerToggle)

        receive_button.setOnClickListener {
            onboardingController.dismiss()
            startActivityFromClass(RequestActivity::class)
        }

        send_button.setOnClickListener {
            startActivityFromClass(CreateTransactionActivity::class)
        }

        fab.setOnClickListener {
            startScanActivityForResult(this)
        }
        transaction_recycler_out.layoutManager = LinearLayoutManager(this)
        transaction_recycler_in.layoutManager = LinearLayoutManager(this)

        currentAddressProvider.observe(this, Observer { address ->
            refreshSubtitle()
        })

        networkDefinitionProvider.observe(this, Observer {
            refreshSubtitle()
        })

        currentTokenProvider.observe(this, Observer {
            setCurrentBalanceObservers()
        })

        val incomingTransactionsAdapter = TransactionRecyclerAdapter(appDatabase, INCOMING, networkDefinitionProvider, exchangeRateProvider, settings)
        transaction_recycler_in.adapter = incomingTransactionsAdapter

        val outgoingTransactionsAdapter = TransactionRecyclerAdapter(appDatabase, OUTGOING, networkDefinitionProvider, exchangeRateProvider, settings)
        transaction_recycler_out.adapter = outgoingTransactionsAdapter

        transactionViewModel.isEmptyViewVisible.nonNull().observe(this) { isEmptyVisible ->
            empty_view_container.setVisibility(isEmptyVisible)
            transaction_recycler_out.setVisibility(!isEmptyVisible)
            transaction_recycler_in.setVisibility(!isEmptyVisible)
        }

        transactionViewModel.incomingLiveData.nonNull().observe(this) { transactions ->
            incomingTransactionsAdapter.submitList(transactions)
            transactionViewModel.hasIncoming.value = !transactions.isNullOrEmpty()
        }

        transactionViewModel.isOnboardingVisible.observe(this, Observer {
            fab.setVisibility(it != true)
        })

        transactionViewModel.outgoingLiveData.observe(this, Observer { transactions ->
            if (transactions != null) {
                outgoingTransactionsAdapter.submitList(transactions)
                transactionViewModel.hasOutgoing.value = !transactions.isNullOrEmpty()
            }
        })


        networkDefinitionProvider.observe(this, Observer {
            setCurrentBalanceObservers()
        })

        currentAddressProvider.observe(this, Observer {
            setCurrentBalanceObservers()
        })

        if (intent.action?.equals("org.walleth.action.SCAN") == true) {
            startScanActivityForResult(this)
        }

        if (savedInstanceState != null) {
            lastPastedData = savedInstanceState.getString(KEY_LAST_PASTED_DATA)
        }
    }

    private fun refreshSubtitle() {
        appDatabase.addressBook.byAddressLiveData(currentAddressProvider.getCurrentNeverNull()).observe(this, Observer { currentAddress ->
            currentAddress?.let { entry ->
                val networkName = networkDefinitionProvider.value!!.getNetworkName()
                supportActionBar?.subtitle = entry.name + "@" + networkName
            }
        })
    }


    private val balanceObserver = Observer<Balance> {
        if (it != null && it.chain == networkDefinitionProvider.getCurrent().chain) {
            amountViewModel.setValue(it.balance, currentTokenProvider.getCurrent())
        } else {
            amountViewModel.setValue(null, currentTokenProvider.getCurrent())
        }
    }

    private val etherObserver = Observer<Balance> {
        if (it != null) {
            send_button.setVisibility(it.balance > ZERO, INVISIBLE)
        } else {
            send_button.visibility = INVISIBLE
        }
    }

    private fun setCurrentBalanceObservers() {
        val currentAddress = currentAddressProvider.value
        if (currentAddress != null) {
            balanceLiveData?.removeObserver(balanceObserver)
            balanceLiveData = appDatabase.balances.getBalanceLive(currentAddress, currentTokenProvider.getCurrent().address, networkDefinitionProvider.getCurrent().chain)
            balanceLiveData?.observe(this, balanceObserver)
            etherLiveData?.removeObserver(etherObserver)
            etherLiveData = appDatabase.balances.getBalanceLive(currentAddress, getRootTokenForChain(networkDefinitionProvider.getCurrent()).address, networkDefinitionProvider.getCurrent().chain)
            etherLiveData?.observe(this, etherObserver)
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
        R.id.menu_copy -> {
            copyToClipboard(currentAddressProvider.getCurrentNeverNull(), fab)
            true
        }
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

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState!!.putString(KEY_LAST_PASTED_DATA, lastPastedData)
    }


    override fun onDestroy() {
        settings.unregisterListener(this)
        super.onDestroy()
    }
}