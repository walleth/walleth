package org.ligi.walleth.activities

import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.lazy
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_in_drawer_container.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.ligi.kaxt.startActivityFromClass
import org.ligi.tracedroid.TraceDroid
import org.ligi.tracedroid.sending.TraceDroidEmailSender
import org.ligi.walleth.App
import org.ligi.walleth.App.Companion.currentAddress
import org.ligi.walleth.R
import org.ligi.walleth.data.*
import org.ligi.walleth.ui.TransactionRecyclerAdapter
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {

    val actionBarDrawerToggle by lazy { ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open, R.string.drawer_close) }
    val bus: EventBus by App.kodein.lazy.instance()

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
            startActivityFromClass(RequestActivity::class.java)
        }

        onEvent(BalanceUpdate)

        send_container.setOnClickListener {
            startActivityFromClass(TransferActivity::class)
        }

        transactionRecycler.adapter = TransactionRecyclerAdapter()
        transactionRecycler.layoutManager = LinearLayoutManager(this)

    }

    fun BigInteger.toEtherValueString(): String {
        val inEther = BigDecimal(this).divide(BigDecimal(ETH_IN_WEI))
        return inEther.round(MathContext(4, RoundingMode.FLOOR)).stripTrailingZeros().toString()
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
            AlertDialog.Builder(this).setMessage("not yet implemented").show()
            true
        }
        else -> actionBarDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(balanceUpdate: BalanceUpdate) {
        currentAddress?.let {
            val balanceForAddress = BalanceProvider.getBalanceForAddress(it)
            current_eth.text = balanceForAddress?.balance?.toEtherValueString() ?: "0"
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: newBlock) {
        val progress = App.syncProgress
        supportActionBar?.subtitle = if (progress != null) {
            val percent = ((progress.currentBlock.toDouble() / progress.highestBlock) * 100).toInt()
            "Block ${progress.currentBlock}/${progress.highestBlock} ($percent%)"
        } else {
            "Block " + App.lastSeenBlock
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: TransactionEvent) {
        transactionRecycler.adapter = TransactionRecyclerAdapter()
    }


}
