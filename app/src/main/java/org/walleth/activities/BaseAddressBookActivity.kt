package org.walleth.activities

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.support.v7.widget.helper.ItemTouchHelper.LEFT
import android.support.v7.widget.helper.ItemTouchHelper.RIGHT
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_list_addresses.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.config.Settings
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.ui.AddressAdapter

abstract class BaseAddressBookActivity : AppCompatActivity() {

    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    val appDatabase: AppDatabase by LazyKodein(appKodein).instance()
    val settings: Settings by LazyKodein(appKodein).instance()
    val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()

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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler_view.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener {
            startActivityFromClass(CreateAccountActivity::class.java)
        }

        starred_only.isChecked = settings.filterAddressesStared
        starred_only.setOnCheckedChangeListener({ _: CompoundButton, isOn: Boolean ->
            settings.filterAddressesStared = isOn
            refresh()
        })

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

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder)
                    = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {

                val current = adapter.displayList[viewHolder.adapterPosition]
                fun changeDeleteState(state: Boolean) {
                    async(UI) {
                        async(CommonPool) {
                            appDatabase.addressBook.upsert(current.apply {
                                deleted = state
                            })
                        }.await()
                    }
                }
                changeDeleteState(true)
                Snackbar.make(coordinator, R.string.deleted_account_snack, Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.undo), { changeDeleteState(false) })
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
        R.id.menu_undelete -> {
            async(UI) {
                async(CommonPool) {
                    appDatabase.addressBook.undeleteAll()
                }.await()
                refresh()
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
