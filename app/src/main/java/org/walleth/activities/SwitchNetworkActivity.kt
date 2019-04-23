package org.walleth.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_list.*
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.networks.ALL_NETWORKS
import org.walleth.data.networks.NetworkDefinition
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.ui.NetworkAdapter

open class SwitchNetworkActivity : BaseSubActivity() {

    val networkDefinitionProvider: NetworkDefinitionProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list)

        supportActionBar?.subtitle = getString(R.string.network_subtitle)

        recycler_view.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener {
            alert(R.string.switch_network_activity_adding_not_yet_supported)
        }
    }

    override fun onResume() {
        super.onResume()
        recycler_view.adapter = getAdapter()
    }

    private fun getAdapter() = NetworkAdapter(ALL_NETWORKS, {
        networkDefinitionProvider.setCurrent(it)
        finish()
    }, { networkDefinition: NetworkDefinition ->
        val uri = Uri.parse(networkDefinition.infoUrl)
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    })

}
