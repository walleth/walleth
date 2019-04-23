package org.walleth.activities

import androidx.lifecycle.Observer
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_list_addresses.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.keystore.api.KeyStore
import org.koin.android.ext.android.inject
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.config.Settings
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.ui.AddressAdapter

abstract class BaseAddressBookActivity : BaseSubActivity() {

    val keyStore: KeyStore by inject()
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
        val anySoftDeletedExists = adapter.list.any { it.deleted }
        menu.findItem(R.id.menu_undelete).isVisible = anySoftDeletedExists
        menu.findItem(R.id.menu_delete_forever).isVisible = anySoftDeletedExists

        menu.findItem(R.id.menu_stared_only).isChecked = settings.filterAddressesStared
        menu.findItem(R.id.menu_only_with_key).isChecked = settings.filterAddressesKeyOnly
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
        R.id.menu_stared_only -> item.filterToggle {
            settings.filterAddressesStared = it
        }
        R.id.menu_only_with_key -> item.filterToggle {
            settings.filterAddressesKeyOnly = it
        }

        R.id.menu_delete_forever -> true.also {
            AlertDialog.Builder(this@BaseAddressBookActivity)
                    .setIcon(R.drawable.ic_warning_orange_24dp)
                    .setTitle(R.string.are_you_sure)
                    .setMessage(R.string.permanent_accounts_delete_confirmation)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        GlobalScope.launch(Dispatchers.Default) {
                            appDatabase.addressBook.run {
                                allDeleted().forEach {
                                    keyStore.deleteKey(it.address)
                                }
                                deleteAllSoftDeleted()
                            }
                        }

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
        else -> super.onOptionsItemSelected(item)
    }


    private fun MenuItem.filterToggle(updater: (value: Boolean) -> Unit) = true.also {
        isChecked = !isChecked
        updater(isChecked)
        refresh()
    }
}
