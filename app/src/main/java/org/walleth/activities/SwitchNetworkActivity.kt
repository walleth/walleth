package org.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_list.*
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.BalanceProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.transactions.TransactionProvider
import org.walleth.ui.NetworkAdapter

open class SwitchNetworkActivity : AppCompatActivity() {

    val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()
    val transactionPovider: TransactionProvider by LazyKodein(appKodein).instance()
    val balanceProvider: BalanceProvider by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list)

        supportActionBar?.subtitle = getString(R.string.address_book_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler_view.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener {
            alert("adding new networks not yet implemented")
        }

        supportActionBar?.subtitle = getString(R.string.address_book_subtitle)
    }

    override fun onResume() {
        super.onResume()
        recycler_view.adapter = getAdapter()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    fun getAdapter() = NetworkAdapter(networkDefinitionProvider.allDefinitions) {
        networkDefinitionProvider.currentDefinition = it
        balanceProvider.clear()
        transactionPovider.clear()
        finish()
    }

}
