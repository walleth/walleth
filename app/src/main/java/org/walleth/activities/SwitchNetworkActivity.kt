package org.walleth.activities

import android.content.Intent
import android.net.Uri
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
import org.walleth.data.networks.AllNetworkDefinitions
import org.walleth.data.networks.NetworkDefinition
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.ui.NetworkAdapter

open class SwitchNetworkActivity : AppCompatActivity() {

    val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()

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

    private fun getAdapter() = NetworkAdapter(AllNetworkDefinitions, {
        networkDefinitionProvider.setCurrent(it)
        finish()
    }, { networkDefinition: NetworkDefinition ->
        val uri = Uri.parse(networkDefinition.infoUrl)
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    })

}
