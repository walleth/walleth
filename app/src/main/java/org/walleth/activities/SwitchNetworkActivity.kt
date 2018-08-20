package org.walleth.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_list.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.networks.ALL_NETWORKS
import org.walleth.data.networks.NetworkDefinition
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.ui.NetworkAdapter

open class SwitchNetworkActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()
    val networkDefinitionProvider: NetworkDefinitionProvider by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list)

        supportActionBar?.subtitle = getString(R.string.network_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler_view.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener {
            alert(R.string.switch_network_activity_adding_not_yet_supported)
        }
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

    private fun getAdapter() = NetworkAdapter(ALL_NETWORKS, {
        networkDefinitionProvider.setCurrent(it)
        finish()
    }, { networkDefinition: NetworkDefinition ->
        val uri = Uri.parse(networkDefinition.infoUrl)
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    })

}
