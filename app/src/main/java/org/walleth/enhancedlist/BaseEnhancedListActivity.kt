package org.walleth.enhancedlist

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.core.animation.doOnEnd
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.ligi.kaxt.startActivityFromURL
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.chains.getFaucetURL
import org.walleth.chains.hasFaucetWithAddressSupport
import org.walleth.data.AppDatabase
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.util.question
import timber.log.Timber

interface Deletable {
    var deleted: Boolean
}

interface ListItem : Deletable {
    val name: String
}

@SuppressLint("Registered")
abstract class BaseEnhancedListActivity<T : ListItem> : BaseSubActivity() {

    abstract val enhancedList: EnhancedListInterface<T>
    abstract val adapter: EnhancedListAdapter<T>

    private var currentSnack: Snackbar? = null
    internal var searchTerm = ""

    fun T.changeDeleteState(state: Boolean) {
        val changedEntity = apply { deleted = state }
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                enhancedList.upsert(changedEntity)
                if (state) {
                    currentSnack = Snackbar.make(coordinator, "Deleted " + changedEntity.name, Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.undo)) { changeDeleteState(false) }
                            .apply { show() }
                }
                refreshAdapter()
                invalidateOptionsMenu()
            }
        }
    }

    internal fun View.deleteWithAnimation(t: T) {
        ObjectAnimator.ofFloat(this, "translationX", this.width.toFloat()).apply {
            duration = 333
            start()
            doOnEnd {
                t.changeDeleteState(true)
                handler.postDelayed({
                    translationX = 0f
                }, 333)
            }
        }
    }

    fun AppCompatImageView.prepareFaucetButton(chainInfo: ChainInfo?,
                                               currentAddressProvider: CurrentAddressProvider,
                                               postAction: () -> Unit = {}) {
        visibility = if (chainInfo?.faucets?.isNotEmpty() == true) {
            setImageResource(if (chainInfo.hasFaucetWithAddressSupport()) R.drawable.ic_flash_on_black_24dp else R.drawable.ic_redeem_black_24dp)
            View.VISIBLE
        } else {
            View.INVISIBLE
        }

        setOnClickListener {
            chainInfo?.getFaucetURL(currentAddressProvider.getCurrentNeverNull())?.let { url ->
                context.startActivityFromURL(url)
                postAction.invoke()
            }
        }
    }

    internal fun MenuItem.filterToggle(updater: (value: Boolean) -> Unit) = true.also {
        isChecked = !isChecked
        updater(isChecked)
        refreshAdapter()
    }

    val appDatabase: AppDatabase by inject()

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val anySoftDeletedExists = adapter.list.any { it.deleted }
        menu.findItem(R.id.menu_undelete).isVisible = anySoftDeletedExists
        menu.findItem(R.id.menu_delete_forever).isVisible = anySoftDeletedExists
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_enhanced_list, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        Timber.i("setting search term $searchTerm")
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?) = true

            override fun onMenuItemActionCollapse(p0: MenuItem?) = true.also {
                searchTerm = ""
            }

        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newSearchTerm: String) = true.also {
                searchTerm = newSearchTerm
                Timber.i("setting search term 2 $newSearchTerm")
                refreshAdapter()
            }

            override fun onQueryTextSubmit(query: String?) = false
        })
        searchView.setQuery(searchTerm, true)

        return super.onCreateOptionsMenu(menu)
    }

    fun checkForSearchTerm(vararg terms: String) = terms.any { it.toLowerCase().contains(searchTerm, ignoreCase = true) }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_undelete -> true.also {
            lifecycleScope.launch(Dispatchers.Main) {
                currentSnack?.dismiss()
                enhancedList.undeleteAll()
                refreshAdapter()
            }
        }
        R.id.menu_delete_forever -> true.also {
            question(configurator = {
                setIcon(R.drawable.ic_warning_orange_24dp)
                setTitle(R.string.are_you_sure)
                setMessage(R.string.permanent_delete_question)
            }, action = {
                currentSnack?.dismiss()
                lifecycleScope.launch {
                    enhancedList.deleteAllSoftDeleted()
                    refreshAdapter()
                }
            })
        }
        else -> super.onOptionsItemSelected(item)
    }


    internal fun refreshAdapter() = lifecycleScope.launch(Dispatchers.Main) {
        adapter.filter(enhancedList.getAll(),
                filter = { enhancedList.filter(it) },
                onChange = { invalidateOptionsMenu() },
                areEqual = { t1, t2 -> enhancedList.compare(t1, t2) })

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list)

        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter

        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, LEFT or RIGHT) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                adapter.displayList[viewHolder.adapterPosition].changeDeleteState(true)
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(recycler_view)
    }

    override fun onResume() {
        super.onResume()
        refreshAdapter()
    }

}