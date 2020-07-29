package org.walleth.overview

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.INVISIBLE
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main_in_drawer_container.*
import kotlinx.android.synthetic.main.activity_overview.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.erc681.toERC681
import org.kethereum.model.EthereumURI
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.ligi.kaxt.livedata.nonNull
import org.ligi.kaxt.livedata.observe
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.WallethActivity
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.AppDatabase
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.balances.Balance
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.getRootToken
import org.walleth.info.WallETHInfoActivity
import org.walleth.qr.scan.QRScanActivityAndProcessActivity
import org.walleth.qr.scan.startScanActivityForResult
import org.walleth.request.RequestActivity
import org.walleth.security.isDappNodeReachable
import org.walleth.security.startOpenVPN
import org.walleth.toolbar.DefaultToolbarChangeDetector
import org.walleth.toolbar.ToolbarColorChangeDetector
import org.walleth.transactions.CreateTransactionActivity
import org.walleth.transactions.TransactionAdapterDirection.INCOMING
import org.walleth.transactions.TransactionAdapterDirection.OUTGOING
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
    private var balanceLiveData: LiveData<Balance>? = null
    private var etherLiveData: LiveData<Balance>? = null
    private val onboardingController by lazy { OnboardingController(this, transactionViewModel, settings) }

    private var lastPastedData: String? = null

    private val amountViewModel by lazy {
        ValueViewController(value_view, exchangeRateProvider, settings)
    }

    override fun onResume() {
        super.onResume()

        if (lastNightMode != null && lastNightMode != settings.getNightMode() ||
                didToolbarColorChange()) {
            recreate()
            return
        }
        lastToolbarColor = calcToolbarColorCombination()
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

        currentAddressProvider.observe(this, Observer {
            refreshSubtitle()
        })

        chainInfoProvider.observe(this, Observer {
            refreshSubtitle()
        })

        currentTokenProvider.observe(this, Observer {
            setCurrentBalanceObservers()
        })

        val incomingTransactionsAdapter = TransactionRecyclerAdapter(appDatabase, INCOMING, chainInfoProvider, exchangeRateProvider, settings)
        transaction_recycler_in.adapter = incomingTransactionsAdapter

        val outgoingTransactionsAdapter = TransactionRecyclerAdapter(appDatabase, OUTGOING, chainInfoProvider, exchangeRateProvider, settings)
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


        chainInfoProvider.observe(this, Observer {
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
                val name = chainInfoProvider.value!!.name
                supportActionBar?.subtitle = entry.name + "@" + name
            }
        })
    }


    private val balanceObserver = Observer<Balance> {
        if (it != null && it.chain == chainInfoProvider.getCurrent()?.chainId) {
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
            balanceLiveData = appDatabase.balances.getBalanceLive(currentAddress, currentTokenProvider.getCurrent().address, chainInfoProvider.getCurrent()?.chainId)
            balanceLiveData?.observe(this, balanceObserver)
            etherLiveData?.removeObserver(etherObserver)
            etherLiveData = appDatabase.balances.getBalanceLive(currentAddress, chainInfoProvider.getCurrent()?.getRootToken()?.address, chainInfoProvider.getCurrent()?.chainId)
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