package org.walleth.chains

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.AppDatabase
import org.walleth.data.chaininfo.ChainInfo
import javax.net.ssl.SSLException

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
            R.id.menu_refresh -> lifecycleScope.launch(Dispatchers.Main) {
                refresh()

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refresh() {
        swipe_refresh_layout.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url("https://chainid.network/chains_mini.json")
                okHttpClient.newCall(request.build()).execute().body()?.string()?.let { json ->
                    moshi.deSerialize(json)
                }?.let { list ->
                    appDatabase.chainInfo.upsert(list)
                }
                delay(1000)
                lifecycleScope.launch(Dispatchers.Main) {
                    setAdapter()
                    swipe_refresh_layout.isRefreshing = false
                }
            } catch (e: SSLException) {
                handleRefreshError("SSLError - cannot load chains. Setting your time can help in some cases.")
            } catch (e: Throwable) {
                handleRefreshError("General Error - cannot load chains. $e")
            }
        }
    }

    private fun handleRefreshError(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            swipe_refresh_layout.isRefreshing = false
            alert(message)
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.Main) {
            setAdapter()
        }
    }

    private suspend fun setAdapter() {
        recycler_view.adapter = withContext(Dispatchers.Default) { getAdapter() }
    }

    private fun getAdapter() = ChainAdapter(appDatabase.chainInfo.getAll(), {
        chainInfoProvider.setCurrent(it)
        finish()
    }, { chainInfo: ChainInfo ->
        val uri = Uri.parse(chainInfo.infoURL)
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    })

}
