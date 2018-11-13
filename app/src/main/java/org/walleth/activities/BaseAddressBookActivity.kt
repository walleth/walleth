package org.walleth.activities

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.support.v7.widget.helper.ItemTouchHelper.LEFT
import android.support.v7.widget.helper.ItemTouchHelper.RIGHT
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import kotlinx.android.synthetic.main.activity_list_addresses.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.config.Settings
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.ui.AddressAdapter

abstract class BaseAddressBookActivity : BaseSubActivity() {

    val keyStore: WallethKeyStore by inject()
    val appDatabase: AppDatabase by inject()
    val settings: Settings by inject()
    val currentAddressProvider: CurrentAddressProvider by inject()

    val adapter by lazy {
        AddressAdapter(keyStore, onClickAction = { onAddressClick(it) }, appDatabase = appDatabase).apply {
            recycler_view.adapter = this
        }
    }

    abstract fun onAddressClick(addressEntry: AddressBookEntry)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list_addresses)

        supportActionBar?.subtitle = getString(R.string.address_book_subtitle)

        recycler_view.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener {
            startActivityFromClass(CreateAccountActivity::class.java)
        }

        starred_only.isChecked = settings.filterAddressesStared
        starred_only.setOnCheckedChangeListener { _: CompoundButton, isOn: Boolean ->
            settings.filterAddressesStared = isOn
            refresh()
        }

        key_only.isChecked = settings.filterAddressesKeyOnly
        key_only.setOnCheckedChangeListener { _: CompoundButton, isOn: Boolean ->
            settings.filterAddressesKeyOnly = isOn
            refresh()
        }

        appDatabase.addressBook.allLiveData().observe(this, Observer { items ->
            if (items != null) {
                adapter.list = items
                refresh()
            }
        })

        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, LEFT or RIGHT) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {

                val current = adapter.displayList[viewHolder.adapterPosition]
                fun changeDeleteState(state: Boolean) {
                    GlobalScope.launch(Dispatchers.Main) {
                        withContext(Dispatchers.Default) {
                            appDatabase.addressBook.upsert(current.apply {
                                deleted = state
                            })
                        }
                    }
                }
                changeDeleteState(true)
                Snackbar.make(coordinator, R.string.deleted_account_snack, Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.undo)) { changeDeleteState(false) }
                        .show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(recycler_view)
    }

    private fun refresh() {
        adapter.filter(settings.filterAddressesStared, settings.filterAddressesKeyOnly)
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_address_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_undelete).isVisible = adapter.list.any { it.deleted }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_undelete -> true.also {
            GlobalScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.Default) {
                    appDatabase.addressBook.undeleteAll()
                }
                refresh()
            }
        }
        else -> super.onOptionsItemSelected(item)
    }
}
