package org.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_account_edit.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.startActivityFromURL
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.addressbook.getByAddressAsync
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider

class EditAccountActivity : AppCompatActivity() {

    private val appDatabase: AppDatabase by LazyKodein(appKodein).instance()
    private val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()
    private val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()
    private lateinit var currentAddressInfo : AddressBookEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_edit)

        supportActionBar?.subtitle = getString(R.string.edit_account_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        appDatabase.addressBook.getByAddressAsync(currentAddressProvider.getCurrent()) {
            currentAddressInfo = it!!


            nameInput.setText(currentAddressInfo.name)
            noteInput.setText(currentAddressInfo.note)

            notification_checkbox.isChecked = currentAddressInfo.isNotificationWanted

            nameInput.doAfterEdit {
                currentAddressInfo.name = nameInput.text.toString()
            }

            noteInput.doAfterEdit {
                currentAddressInfo.note = noteInput.text.toString()
            }

            notification_checkbox.setOnCheckedChangeListener { _, isChecked ->
                currentAddressInfo.isNotificationWanted = isChecked
            }
        }
    }

    override fun onPause() {
        super.onPause()
        async(CommonPool) {
            appDatabase.addressBook.upsert(currentAddressInfo)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?)
            = super.onCreateOptionsMenu(menu.apply { menuInflater.inflate(R.menu.menu_edit, menu) })


    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_etherscan -> {
            startActivityFromURL(networkDefinitionProvider.value!!.getBlockExplorer().getURLforAddress(currentAddressProvider.getCurrent()))
            true
        }
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
