package org.walleth.activities

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
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.keystore.WallethKeyStore

abstract class BaseAddressBookActivity : AppCompatActivity() {

    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    val appDatabase: AppDatabase by LazyKodein(appKodein).instance()

    var notDeletedEntries: List<AddressBookEntry> = emptyList()
    var deletedEntries: List<AddressBookEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list)

        supportActionBar?.subtitle = getString(R.string.address_book_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler_view.layoutManager = LinearLayoutManager(this)

        fab.setOnClickListener {
            startActivityFromClass(CreateAccountActivity::class.java)
        }

        refresh()

        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, LEFT or RIGHT) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder)
                    = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                val current = notDeletedEntries.getOrNull(viewHolder.adapterPosition)
                if (current != null) {
                    fun changeDeleteState(state: Boolean) {async(UI) {
                        async(CommonPool) {
                            current.deleted = state
                            appDatabase.addressBook.upsert(current)
                        }.await()
                        refresh()
                    }}
                    changeDeleteState(true)
                    Snackbar.make(coordinator, R.string.deleted_account_snack, Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.undo), { changeDeleteState(false) })
                            .show()
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(recycler_view)
    }

    abstract fun setupAdapter()

    protected fun refresh() {
        setupAdapter()
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_address_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_undelete).isVisible = deletedEntries.isNotEmpty()
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
