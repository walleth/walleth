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
import org.kethereum.functions.isERC67String
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
import org.walleth.data.networks.BaseCurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.syncprogress.SyncProgressProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.transactions.TransactionProvider
import org.walleth.ui.ChangeObserver
import org.walleth.ui.TransactionAdapterDirection.INCOMMING
import org.walleth.ui.TransactionAdapterDirection.OUTGOING
import org.walleth.ui.TransactionRecyclerAdapter


class MainActivity : AppCompatActivity() {

    val lazyKodein = LazyKodein(appKodein)

    val actionBarDrawerToggle by lazy { ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open, R.string.drawer_close) }

    val transactionProvider: TransactionProvider by lazyKodein.instance()
    val syncProgressProvider: SyncProgressProvider by lazyKodein.instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by lazyKodein.instance()
    val keyStore: WallethKeyStore by lazyKodein.instance()
    val appDatabase: AppDatabase by lazyKodein.instance()
    val settings: Settings by lazyKodein.instance()
    val currentTokenProvider: CurrentTokenProvider by lazyKodein.instance()
    val currentAddressProvider: BaseCurrentAddressProvider by lazyKodein.instance()
    var lastNightMode: Int? = null

    var balanceLiveData: LiveData<Balance>? = null

    override fun onResume() {
        super.onResume()

        if (lastNightMode != null && lastNightMode != settings.getNightMode()) {
            recreateWhenPossible()
            return
        }
        lastNightMode = settings.getNightMode()
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
            alert(title = "Special Awareness", message = "Please note this is one alpha on the rinkeby test-network. Please do not work with real values yet!")
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

        networkDefinitionProvider.observe(this, Observer {
            setCurrentBalanceObserver()
        })


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
                val currentAddress = currentAddressProvider.value
                if (currentAddress != null) {
                    val allTransactions = transactionProvider.getTransactionsForAddress(currentAddress)
                    val incomingTransactions = allTransactions.filter { it.transaction.to == currentAddress }.sortedByDescending { it.transaction.creationEpochSecond }
                    val outgoingTransactions = allTransactions.filter { it.transaction.from == currentAddress }.sortedByDescending { it.transaction.creationEpochSecond }

                    val hasNoTransactions = incomingTransactions.size + outgoingTransactions.size == 0

                    runOnUiThread {
                        transaction_recycler_out.adapter = TransactionRecyclerAdapter(outgoingTransactions, appDatabase, OUTGOING)
                        transaction_recycler_in.adapter = TransactionRecyclerAdapter(incomingTransactions, appDatabase, INCOMMING)

                        empty_view_container.setVisibility(hasNoTransactions)

                        send_container.setVisibility(!hasNoTransactions, INVISIBLE)

                        transaction_recycler_in.setVisibility(!hasNoTransactions)
                        transaction_recycler_out.setVisibility(!hasNoTransactions)

                    }
                }
            }
        })


        currentAddressProvider.observe(this, Observer { _ ->
            setCurrentBalanceObserver()
        })
//
        /*
        balanceProvider.registerChangeObserverWithInitialObservation(object : ChangeObserver {
            override fun observeChange() {
                var balanceForAddress = Balance(balance = BigInteger("0"), block = 0, token = currentTokenProvider.currentToken)
                val address = currentAddressProvider.value
                if (address != null) {
                    balanceProvider.getBalanceForAddress(address, currentTokenProvider.currentToken)?.let {
                        balanceForAddress = it
                    }

                    runOnUiThread {


                        if (!syncProgressProvider.currentSyncProgress.isSyncing) {
                            supportActionBar?.subtitle = "Block " + balanceForAddress.block
                        }
                    }
                }
            }
        })
        */

    }


    private val balanceObserver = Observer<Balance> {
        it?.let { balance ->
            balance.chain
            value_view.setValue(balance.balance, currentTokenProvider.currentToken)
            supportActionBar?.subtitle = "Block " + it.block

        }
    }

    private fun setCurrentBalanceObserver() {
        val currentAddress = currentAddressProvider.value
        if (currentAddress !=null) {
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
