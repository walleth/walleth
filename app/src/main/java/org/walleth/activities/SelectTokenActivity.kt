package org.walleth.activities

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import kotlinx.android.synthetic.main.activity_list_stars.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
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

class SelectTokenActivity : BaseSubActivity() {

    private val currentTokenProvider: CurrentTokenProvider by inject()
    private val networkDefinitionProvider: NetworkDefinitionProvider by inject()
    private val appDatabase: AppDatabase by inject()
    private val settings: Settings by inject()

    private var showDelete = false

    private val viewModel by lazy {
        ViewModelProviders.of(this).get(TokenActivityViewModel::class.java)
    }

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

        recycler_view.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener {
            startActivityFromClass(CreateTokenDefinitionActivity::class)
        }

        starred_only.setOnCheckedChangeListener { compoundButton: CompoundButton, isOn: Boolean ->
            tokenListAdapter.filter(viewModel.searchTerm, isOn)
            settings.showOnlyStaredTokens = isOn
        }

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
                    GlobalScope.launch {
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
            GlobalScope.launch {
                appDatabase.tokens.showAll()
            }
        }
        else -> super.onOptionsItemSelected(item)
    }

}