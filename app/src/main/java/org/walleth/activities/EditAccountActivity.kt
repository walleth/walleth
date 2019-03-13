package org.walleth.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_account_edit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.startActivityFromURL
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.addressbook.getByAddressAsync
import org.walleth.data.blockexplorer.BlockExplorerProvider
import org.walleth.util.copyToClipboard

const val INTENT_KEY_ADDRESS = "ADDRESS"

class EditAccountActivity : AddressReceivingActivity() {

    private val appDatabase: AppDatabase by inject()

    private val blockExplorerProvider: BlockExplorerProvider by inject()
    private lateinit var currentAddressInfo: AddressBookEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_edit)

        supportActionBar?.subtitle = getString(R.string.edit_account_subtitle)

        appDatabase.addressBook.getByAddressAsync(relevantAddress) {
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
        GlobalScope.launch(Dispatchers.Default) {
            appDatabase.addressBook.upsert(currentAddressInfo)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?) = super.onCreateOptionsMenu(menu.apply { menuInflater.inflate(R.menu.menu_edit, menu) })


    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_copy -> true.also {
            copyToClipboard(relevantAddress, activity_main)
        }
        R.id.menu_etherscan -> true.also {
            startActivityFromURL(blockExplorerProvider.get().getAddressURL(relevantAddress))
        }
        else -> super.onOptionsItemSelected(item)
    }
}