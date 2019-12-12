package org.walleth.chains

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
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxt.startActivityFromURL
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.AppDatabase
import javax.net.ssl.SSLException

open class SwitchChainActivity : BaseSubActivity() {

    val chainInfoProvider: ChainInfoProvider by inject()
    val appDatabase: AppDatabase by inject()
    private val okHttpClient: OkHttpClient by inject()
    private val moshi: Moshi by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list)

        supportActionBar?.subtitle = getString(R.string.chains_subtitle)

        recycler_view.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener {
            startActivityFromClass(EditChainActivity::class)
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
                    appDatabase.chainInfo.insertIfDoesNotExist(list)
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

    private fun getAdapter() = ChainAdapter(appDatabase.chainInfo.getAll()) { chainInfo, action ->

        when(action) {
            ChainInfoViewAction.CLICK -> {
                chainInfoProvider.setCurrent(chainInfo)
                finish()
            }
            ChainInfoViewAction.EDIT -> {
                startEditChainActivity(chainInfo)
            }
            ChainInfoViewAction.INFO -> {
                startActivityFromURL(chainInfo.infoURL)
            }
        }
    }

}
