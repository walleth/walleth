package org.walleth.tokens

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.android.synthetic.main.token_list_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.AppDatabase
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.Token
import org.walleth.enhancedlist.BaseEnhancedListActivity
import org.walleth.enhancedlist.EnhancedListAdapter
import org.walleth.enhancedlist.EnhancedListInterface

class SelectTokenActivity : BaseEnhancedListActivity<Token>() {

    private val chainInfoProvider: ChainInfoProvider by inject()
    private val currentTokenProvider: CurrentTokenProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = getString(R.string.select_token_activity_select_token)

        fab.setOnClickListener {
            startActivityFromClass(CreateTokenDefinitionActivity::class)
        }

    }

    suspend fun upsert(appDatabase: AppDatabase, token: Token) {
        appDatabase.tokens.upsert(token)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_tokenlist, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_stared_only).isChecked = settings.showOnlyStaredTokens
        menu.findItem(R.id.menu_current_network_only).isChecked = settings.showOnlyTokensOnCurrentNetwork
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_stared_only -> item.filterToggle {
            settings.showOnlyStaredTokens = it
        }

        R.id.menu_current_network_only -> item.filterToggle {
            settings.showOnlyTokensOnCurrentNetwork = it
        }
        else -> super.onOptionsItemSelected(item)
    }

    override val enhancedList by lazy {
        object : EnhancedListInterface<Token> {
            override suspend fun undeleteAll() = appDatabase.tokens.unDeleteAll()
            override suspend fun getAll() = appDatabase.tokens.all()
            override fun compare(t1: Token, t2: Token) = t1.address == t2.address
            override suspend fun upsert(item: Token) = appDatabase.tokens.upsert(item)
            override suspend fun deleteAllSoftDeleted() {
                lifecycleScope.launch(Dispatchers.Default) {
                    appDatabase.tokens.deleteAllSoftDeleted()
                }
            }

            override suspend fun filter(item: Token) = (!settings.showOnlyStaredTokens || item.starred)
                    && (!settings.showOnlyTokensOnCurrentNetwork || item.chain == chainInfoProvider.getCurrentChainId().value)
                    && checkForSearchTerm(item.name, item.symbol)
        }
    }

    override val adapter: EnhancedListAdapter<Token> by lazy {
        EnhancedListAdapter<Token>(
                layout = R.layout.token_list_item,
                bind = { entry, view ->

                    lifecycleScope.launch(Dispatchers.Main) {
                        val chainInfo = appDatabase.chainInfo.getByChainId(entry.chain)
                        view.token_chain.text = "Chain: " + chainInfo?.name

                        view.token_name.text = "${entry.name}(${entry.symbol})"
                        view.delete_button.setOnClickListener {
                            view.deleteWithAnimation(entry)
                        }

                        view.token_starred_button.setImageResource(
                                if (entry.starred) {
                                    R.drawable.ic_star_24dp
                                } else {
                                    R.drawable.ic_star_border_24dp
                                }
                        )

                        view.token_starred_button.setOnClickListener {
                            lifecycleScope.launch {
                                val updatedEntry = entry.copy(starred = !entry.starred)
                                appDatabase.tokens.upsert(updatedEntry)
                                refreshAdapter()
                            }
                        }

                        view.setOnClickListener {
                            if (chainInfo == null) {
                                alert("Chain for this token not found")
                            } else {
                                lifecycleScope.launch(Dispatchers.Default) {
                                    currentTokenProvider.setCurrent(entry)
                                    chainInfoProvider.setCurrent(chainInfo)
                                }
                                finish()
                            }                        }
                    }

                }
        )

    }
}