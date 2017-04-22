package org.ligi.walleth.activities

import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
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
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromClass
import org.ligi.tracedroid.TraceDroid
import org.ligi.tracedroid.sending.TraceDroidEmailSender
import org.ligi.walleth.App
import org.ligi.walleth.App.Companion.currentAddress
import org.ligi.walleth.R
import org.ligi.walleth.data.BalanceAtBlock
import org.ligi.walleth.data.BalanceProvider
import org.ligi.walleth.data.TransactionEvent
import org.ligi.walleth.data.TransactionProvider
import org.ligi.walleth.data.syncprogress.SyncProgressProvider
import org.ligi.walleth.functions.toEtherValueString
import org.ligi.walleth.ui.ChangeObserver
import org.ligi.walleth.ui.TransactionRecyclerAdapter
import java.math.BigInteger


class MainActivity : AppCompatActivity() {

    val lazyKodein = LazyKodein(appKodein)

    val actionBarDrawerToggle by lazy { ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open, R.string.drawer_close) }
    val bus: EventBus by lazyKodein.instance()
    val balanceProvider: BalanceProvider by lazyKodein.instance()
    val transactionProvider: TransactionProvider by lazyKodein.instance()

    val syncProgressProvider: SyncProgressProvider by lazyKodein.instance()

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


        balanceProvider.registerChangeObserverWithInitialObservation(object : ChangeObserver {
            override fun observeChange() {
                var balanceForAddress = BalanceAtBlock(balance = BigInteger("0"), block = 0)
                currentAddress?.let {
                    balanceProvider.getBalanceForAddress(it)?.let {
                        balanceForAddress = it
                    }
                }
                val balanceIsZero = balanceForAddress.balance == BigInteger.ZERO

                runOnUiThread {
                    current_eth.text = balanceForAddress.balance.toEtherValueString()
                    current_fiat.text = "0"

                    send_container.setVisibility(!balanceIsZero, INVISIBLE)
                    empty_view.setVisibility(balanceIsZero)

                    transactionRecyclerIn.setVisibility(!balanceIsZero)
                    transactionRecyclerOut.setVisibility(!balanceIsZero)

                    if (!syncProgressProvider.currentSyncProgress.isSyncing) {
                        supportActionBar?.subtitle = "Block " + balanceForAddress.block
                    }
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // don't want too many windows in worst case - so check for errors first
        if (TraceDroid.getStackTraceFiles().isNotEmpty()) {
            TraceDroidEmailSender.sendStackTraces("ligi@ligi.de", this)
        }

        if (App.keyStore.accounts.size() == 0L) {
            startActivityFromClass(CreateAccountActivity::class.java)
            finish()
        } else {
            onCreateAfterPreChecks()
        }

        bus.register(this)
    }

    override fun onDestroy() {
        bus.unregister(this)

        super.onDestroy()
    }

    private fun onCreateAfterPreChecks() {
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

        transactionRecyclerOut.layoutManager = LinearLayoutManager(this)
        transactionRecyclerOut.adapter = TransactionRecyclerAdapter(transactionProvider)
        transactionRecyclerIn.layoutManager = LinearLayoutManager(this)

        transactionRecyclerIn.adapter = TransactionRecyclerAdapter(transactionProvider)
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: TransactionEvent) {

        //transactionRecyclerIn.adapter = TransactionRecyclerAdapter(transactionProvider)
    }

}
