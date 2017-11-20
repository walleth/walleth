package org.walleth.activities

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.helper.ItemTouchHelper
import android.support.v7.widget.helper.ItemTouchHelper.LEFT
import android.support.v7.widget.helper.ItemTouchHelper.RIGHT
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.ui.TokenListAdapter

class SelectTokenActivity : AppCompatActivity() {

    private val currentTokenProvider: CurrentTokenProvider by LazyKodein(appKodein).instance()
    private val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()
    private val appDatabase: AppDatabase by LazyKodein(appKodein).instance()

    private var showDelete = false

    private val tokenListAdapter by lazy {
        TokenListAdapter(currentTokenProvider, this).apply {
            recycler_view.adapter = this
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list)

        async(CommonPool) {
            appDatabase.tokens.showAll()
        }

        supportActionBar?.subtitle = getString(R.string.select_token_activity_select_token)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler_view.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener {
            startActivityFromClass(CreateTokenDefinitionActivity::class)
        }

        appDatabase.tokens.allForChainLive(networkDefinitionProvider.value!!.chain).observe(this, Observer { allTokens ->

            if (allTokens != null) {
                tokenListAdapter.updateTokenList(allTokens.filter { it.showInList }, "")
                showDelete = allTokens.any { !it.showInList }
            }
            invalidateOptionsMenu()
        })

        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, LEFT or RIGHT) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                tokenListAdapter.tokenList.getOrNull(viewHolder.adapterPosition)?.let { currentNotNull ->
                    fun changeDeleteState(state: Boolean) {
                        async(UI) {
                            async(CommonPool) {
                                appDatabase.tokens.upsert(currentNotNull.copy(showInList = state))
                            }.await()
                        }
                    }
                    changeDeleteState(false)
                    val snackMessage = getString(R.string.deleted_token_snack, currentNotNull.symbol)
                    Snackbar.make(coordinator, snackMessage, Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.undo), { changeDeleteState(true) })
                            .show()
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(recycler_view)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_tokenlist, menu)

        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(searchTerm: String): Boolean {
                if (searchTerm != null) {
                    tokenListAdapter.filter(searchTerm)
                }
                return true
            }

            override fun onQueryTextSubmit(query: String?) = true
        })

        return super.onCreateOptionsMenu(menu)
    }


    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_undelete).isVisible = showDelete
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_undelete -> {
            async(UI) {
                async(CommonPool) {
                    appDatabase.tokens.showAll()
                }.await()
            }

            true
        }
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

}

