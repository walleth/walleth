package org.walleth.activities

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
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
import android.widget.CompoundButton
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_list_stars.*
import kotlinx.coroutines.experimental.launch
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.config.Settings
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.Token
import org.walleth.ui.TokenListAdapter

class TokenActivityViewModel : ViewModel() {
    var searchTerm: String = ""
}

class SelectTokenActivity : AppCompatActivity() {

    private val currentTokenProvider: CurrentTokenProvider by LazyKodein(appKodein).instance()
    private val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()
    private val appDatabase: AppDatabase by LazyKodein(appKodein).instance()
    private val settings: Settings by LazyKodein(appKodein).instance()

    private var showDelete = false

    private val viewModel by lazy { ViewModelProviders.of(this).get(TokenActivityViewModel::class.java) }

    private val tokenListAdapter by lazy {
        TokenListAdapter(currentTokenProvider, this, appDatabase).apply {
            recycler_view.adapter = this
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list_stars)

        starred_only.isChecked = settings.showOnlyStaredTokens

        supportActionBar?.subtitle = getString(R.string.select_token_activity_select_token)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler_view.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener {
            startActivityFromClass(CreateTokenDefinitionActivity::class)
        }

        starred_only.setOnCheckedChangeListener({ compoundButton: CompoundButton, isOn: Boolean ->
            tokenListAdapter.filter(viewModel.searchTerm, isOn)
            settings.showOnlyStaredTokens = isOn
        })

        appDatabase.tokens.allForChainLive(networkDefinitionProvider.value!!.chain).observe(this, Observer { allTokens ->

            if (allTokens != null) {
                tokenListAdapter.updateTokenList(allTokens.filter { it.showInList }, viewModel.searchTerm, starred_only.isChecked)
                showDelete = allTokens.any { !it.showInList }
            }
            invalidateOptionsMenu()
        })

        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, LEFT or RIGHT) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                val currentToken = tokenListAdapter.sortedList.get(viewHolder.adapterPosition)
                fun changeDeleteState(state: Boolean) {
                    launch {
                        upsert(appDatabase, currentToken.copy(showInList = state))
                    }
                }
                changeDeleteState(false)
                val snackMessage = getString(R.string.deleted_token_snack, currentToken.symbol)
                Snackbar.make(coordinator, snackMessage, Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.undo), { changeDeleteState(true) })
                        .show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(recycler_view)
    }

    fun upsert(appDatabase: AppDatabase, token: Token) {
        appDatabase.tokens.upsert(token)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_tokenlist, menu)


        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        val lastTerm = viewModel.searchTerm
        if (!lastTerm.isBlank()) {
            searchItem.expandActionView()
        }

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?) = true

            override fun onMenuItemActionCollapse(p0: MenuItem?) = true.also {
                viewModel.searchTerm = ""
            }

        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(searchTerm: String) = true.also {
                tokenListAdapter.filter(searchTerm, starred_only.isChecked)
                if (!searchTerm.isBlank()) {
                    viewModel.searchTerm = searchTerm
                }
            }

            override fun onQueryTextSubmit(query: String?) = false
        })
        searchView.setQuery(lastTerm, true)

        return super.onCreateOptionsMenu(menu)
    }


    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_undelete).isVisible = showDelete

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_undelete -> true.also {
            launch {
                appDatabase.tokens.showAll()
            }
        }
        android.R.id.home -> true.also {
            finish()
        }
        else -> super.onOptionsItemSelected(item)
    }

}