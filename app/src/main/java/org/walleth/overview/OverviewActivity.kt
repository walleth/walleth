package org.walleth.overview

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.INVISIBLE
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main_in_drawer_container.*
import kotlinx.android.synthetic.main.activity_overview.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.erc681.toERC681
import org.kethereum.model.EthereumURI
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.WallethActivity
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.AppDatabase
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.getRootToken
import org.walleth.info.WallETHInfoActivity
import org.walleth.qr.scan.QRScanActivityAndProcessActivity
import org.walleth.request.RequestActivity
import org.walleth.security.isDappNodeReachable
import org.walleth.security.startOpenVPN
import org.walleth.toolbar.DefaultToolbarChangeDetector
import org.walleth.toolbar.ToolbarColorChangeDetector
import org.walleth.transactions.CreateTransactionActivity
import org.walleth.transactions.TransactionAdapterDirection.*
import org.walleth.transactions.TransactionRecyclerAdapter
import org.walleth.util.copyToClipboard
import org.walleth.valueview.ValueViewController
import java.math.BigInteger.ZERO

private const val KEY_LAST_PASTED_DATA: String = "LAST_PASTED_DATA"

class OverviewActivity : WallethActivity(), OnSharedPreferenceChangeListener, ToolbarColorChangeDetector by DefaultToolbarChangeDetector() {

    private val actionBarDrawerToggle by lazy { ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open, R.string.drawer_close) }

    private val chainInfoProvider: ChainInfoProvider by inject()
    private val appDatabase: AppDatabase by inject()
    private val currentTokenProvider: CurrentTokenProvider by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val exchangeRateProvider: ExchangeRateProvider by inject()

    private val transactionViewModel: TransactionListViewModel by viewModel()

    private var lastNightMode: Int? = null
    private var balanceFlowCollectorJob: Job? = null
    private var etherLiveData: Job? = null
    private val onboardingController by lazy { OnboardingController(this, transactionViewModel, settings) }

    private var lastPastedData: String? = null

    private val amountViewModel by lazy {
        ValueViewController(value_view, exchangeRateProvider, settings)
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT <21) {
            alert("Support for devices with an Android version prior to 5 has ended because we want to use jetpack compose. Please export your key(s) and use it in another wallet or fork the wallet to continue support for older Android versions.")
        }
        if (lastNightMode != null && lastNightMode != settings.getNightMode() ||
                didToolbarColorChange()) {
            recreate()
            return
        }
        lastToolbarColor = calcToolbarColorCombination()
        lastNightMode = settings.getNightMode()

        lifecycleScope.launch(Dispatchers.Main) {
            setCurrentBalanceObservers()
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val item = clipboard.primaryClip?.let { it.getItemAt(0).text?.toString() }
            val erc681 = item?.let { EthereumURI(it).toERC681() }
            if (erc681?.valid == true && erc681.address != null && item != lastPastedData && item != currentAddressProvider.getCurrent()?.hex.let { ERC681(address = it).generateURL() }) {
                lastPastedData = item
                Snackbar.make(fab, R.string.paste_from_clipboard, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.paste_from_clipboard_action) {
                            alert(R.string.copied_string_warning_message, R.string.copied_string_warning_title) {
                                startActivity(Intent(this@OverviewActivity, CreateTransactionActivity::class.java).apply {
                                    data = Uri.parse(item)
                                })
                            }

                        }
                        .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            if (!isDappNodeReachable())
                lifecycleScope.launch(Dispatchers.Main) {
                    if (settings.dappNodeAutostartVPN) {
                        startOpenVPN(settings)
                    }
                }
        }

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
            startActivityFromClass(QRScanActivityAndProcessActivity::class.java)
        }
        transaction_recycler_out.layoutManager = LinearLayoutManager(this)
        transaction_recycler_in.layoutManager = LinearLayoutManager(this)


        val incomingTransactionsAdapter = TransactionRecyclerAdapter(appDatabase, INCOMING, chainInfoProvider, exchangeRateProvider, settings)
        transaction_recycler_in.adapter = incomingTransactionsAdapter

        val outgoingTransactionsAdapter = TransactionRecyclerAdapter(appDatabase, OUTGOING, chainInfoProvider, exchangeRateProvider, settings)
        transaction_recycler_out.adapter = outgoingTransactionsAdapter

        lifecycleScope.launch(Dispatchers.Main) {
            chainInfoProvider.getFlow().onEach {
                refreshSubtitle()
                setCurrentBalanceObservers()
            }.launchIn(lifecycleScope)

            currentAddressProvider.flow.onEach {
                refreshSubtitle()
                setCurrentBalanceObservers()
            }.launchIn(lifecycleScope)

            currentTokenProvider.getFlow().onEach {
                setCurrentBalanceObservers()
            }.launchIn(lifecycleScope)


            var incomingAdapterJob : Job? = null
            transactionViewModel.getIncomingTransactionsPager().onEach { pager ->
                incomingAdapterJob?.cancel()
                incomingAdapterJob = pager.flow.onEach {
                    incomingTransactionsAdapter.submitData(it)
                }.launchIn(lifecycleScope)
            }.launchIn(lifecycleScope)

            var outgoingAdapterJob : Job? = null
            transactionViewModel.getOutgoingTransactionsPager().onEach { pager ->
                outgoingAdapterJob?.cancel()
                outgoingAdapterJob = pager.flow.onEach {
                    outgoingTransactionsAdapter.submitData(it)
                }.launchIn(lifecycleScope)
            }.launchIn(lifecycleScope)

        }

        lifecycleScope.launch(Dispatchers.IO) {
            transactionViewModel.isEmptyViewVisibleFlow().collect { isEmptyVisible ->
                lifecycleScope.launch(Dispatchers.Main) {
                    empty_view_container.setVisibility(isEmptyVisible)
                    transaction_recycler_out.setVisibility(!isEmptyVisible)
                    transaction_recycler_in.setVisibility(!isEmptyVisible)
                }
            }
        }

        transactionViewModel.isOnboardingVisible.observe(this, {
            fab.setVisibility(it != true)
        })


        if (savedInstanceState != null) {
            lastPastedData = savedInstanceState.getString(KEY_LAST_PASTED_DATA)
        }
    }

    var refreshActionBarJob: Job? = null
    private fun refreshSubtitle() {
        refreshActionBarJob?.cancel()
        val currentAddress = currentAddressProvider.getCurrentNeverNull()
        refreshActionBarJob = appDatabase.addressBook.byAddressFlow(currentAddress).onEach { currentEntry ->
            currentEntry?.let { entry ->
                lifecycleScope.launch(Dispatchers.Main) {
                    val name = chainInfoProvider.getCurrent().name
                    supportActionBar?.subtitle = entry.name + "@" + name
                }
            }
        }.launchIn(lifecycleScope)
    }

    private suspend fun setCurrentBalanceObservers() {
        val currentAddress = currentAddressProvider.getCurrent()
        if (currentAddress != null) {
            balanceFlowCollectorJob?.cancel()
            balanceFlowCollectorJob = lifecycleScope.launch {
                val tokenAddress = currentTokenProvider.getCurrent().address
                appDatabase.balances.getBalanceLive(currentAddress, tokenAddress, chainInfoProvider.getCurrent().chainId).filterNotNull().collect {
                    if (it.chain == chainInfoProvider.getCurrent().chainId) {
                        amountViewModel.setValue(it.balance, currentTokenProvider.getCurrent())
                    } else {
                        amountViewModel.setValue(null, currentTokenProvider.getCurrent())
                    }
                }
            }

            etherLiveData?.cancel()
            etherLiveData = lifecycleScope.launch {
                appDatabase.balances.getBalanceLive(currentAddress, chainInfoProvider.getCurrent().getRootToken().address, chainInfoProvider.getCurrent().chainId).filterNotNull().collect {
                    send_button.setVisibility(it.balance > ZERO, INVISIBLE)
                }
            }
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
            startActivityFromClass(WallETHInfoActivity::class.java)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_LAST_PASTED_DATA, lastPastedData)
    }


    override fun onDestroy() {
        settings.unregisterListener(this)
        super.onDestroy()
    }
}