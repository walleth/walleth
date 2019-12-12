package org.walleth.chains

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.coroutines.Dispatchers
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
import org.walleth.chains.ChainInfoViewAction.*
import org.walleth.data.AppDatabase
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.util.question
import javax.net.ssl.SSLException

open class SwitchChainActivity : BaseSubActivity() {

    fun ChainInfo.changeDeleteState(state: Boolean) {
        val changedChainInfo = apply { softDeleted = state }
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                appDatabase.chainInfo.upsert(changedChainInfo)
                if (state) {
                    Snackbar.make(coordinator, "Deleted Chain " + changedChainInfo.name, Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.undo)) { changeDeleteState(false) }
                            .show()
                }
                refreshAdapter()
            }
        }
    }

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
            downloadNewChains()
        }

        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, LEFT or RIGHT) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                chainAdapter.displayList[viewHolder.adapterPosition].changeDeleteState(true)
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(recycler_view)

        recycler_view.adapter = chainAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chain_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val anySoftDeletedExists = chainAdapter.list.any { it.softDeleted }
        menu.findItem(R.id.menu_undelete).isVisible = anySoftDeletedExists
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        lifecycleScope.launch(Dispatchers.Main) {
            when (item.itemId) {
                R.id.menu_refresh -> downloadNewChains()
                R.id.menu_undelete -> {
                    appDatabase.chainInfo.unDeleteAll()
                    refreshAdapter()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun downloadNewChains() {
        swipe_refresh_layout.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url("https://chainid.network/chains_mini.json")
                val list = okHttpClient.newCall(request.build()).execute().body()?.string()?.let { json ->
                    moshi.deSerialize(json)
                }
                val newList = list?.filter { appDatabase.chainInfo.getByChainId(it.chainId) == null }
                lifecycleScope.launch(Dispatchers.Main) {

                    when {
                        newList == null -> handleRefreshError("cannot load chains")
                        newList.isEmpty() -> handleRefreshError("no new chains found")
                        else -> {
                            question(configurator = { setMessage("Really Import ${newList.size} Elements?") }, action = {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    appDatabase.chainInfo.upsert(newList)
                                    refreshAdapter()
                                }
                            })
                            swipe_refresh_layout.isRefreshing = false
                        }
                    }

                }

            } catch (e: SSLException) {
                handleRefreshError("SSLError - cannot load chains. Setting your time can help in some cases.")
            } catch (e: Throwable) {
                handleRefreshError("General Error - cannot load chains. $e")
            }
        }
    }

    private fun handleRefreshError(message: String) = lifecycleScope.launch(Dispatchers.Main) {
        swipe_refresh_layout.isRefreshing = false
        alert(message)
    }

    override fun onResume() {
        super.onResume()
        refreshAdapter()
    }

    private fun refreshAdapter() = lifecycleScope.launch(Dispatchers.Main) {
        chainAdapter.filter(appDatabase.chainInfo.getAll(), false)
        invalidateOptionsMenu()
    }

    private val chainAdapter: ChainAdapter by lazy {
        ChainAdapter { chainInfo, action ->

            when (action) {
                CLICK -> {
                    chainInfoProvider.setCurrent(chainInfo)
                    finish()
                }
                EDIT -> startEditChainActivity(chainInfo)
                INFO -> startActivityFromURL(chainInfo.infoURL)
                DELETE -> {
                    lifecycleScope.launch(Dispatchers.Main) {
                        chainInfo.changeDeleteState(true)
                    }
                }
            }
        }

    }
}
