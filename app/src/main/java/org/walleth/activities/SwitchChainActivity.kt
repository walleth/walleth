package org.walleth.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.data.networks.ChainInfoProvider
import org.walleth.data.networks.deSerialize
import org.walleth.ui.NetworkAdapter

open class SwitchChainActivity : BaseSubActivity() {

    val chainInfoProvider: ChainInfoProvider by inject()
    val appDatabase: AppDatabase by inject()
    private val okHttpClient: OkHttpClient by inject()
    private val moshi: Moshi by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list)

        supportActionBar?.subtitle = getString(R.string.network_subtitle)

        recycler_view.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener {
            alert(R.string.switch_network_activity_adding_not_yet_supported)
        }

        swipe_refresh_layout.setOnRefreshListener {
            refresh()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chain_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> GlobalScope.launch(Dispatchers.Main) {
                refresh()

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refresh() {
        swipe_refresh_layout.isRefreshing = true
        GlobalScope.launch(Dispatchers.IO) {
            val request = Request.Builder().url("https://chainid.network/chains_mini.json")
            okHttpClient.newCall(request.build()).execute().body()?.string()?.let { json ->
                moshi.deSerialize(json)
            }?.let { list ->
                appDatabase.chainInfo.upsert(list)
            }
            delay(1000)
            GlobalScope.launch(Dispatchers.Main) {
                setAdapter()
                swipe_refresh_layout.isRefreshing = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        GlobalScope.launch(Dispatchers.Main) {
            setAdapter()
        }
    }

    private suspend fun setAdapter() {
        recycler_view.adapter = withContext(Dispatchers.Default) { getAdapter() }
    }

    private fun getAdapter() = NetworkAdapter(appDatabase.chainInfo.getAll(), {
        chainInfoProvider.setCurrent(it)
        finish()
    }, { chainInfo: ChainInfo ->
        val uri = Uri.parse(chainInfo.infoURL)
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    })

}
