package org.ligi.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_account_edit.*
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.startActivityFromURL
import org.ligi.walleth.App
import org.ligi.walleth.R
import org.ligi.walleth.data.addressbook.AddressBook

class EditAccountActivity : AppCompatActivity() {

    val addressBook: AddressBook by LazyKodein(appKodein).instance()

    val currentAddressInfo by lazy { addressBook.getEntryForName(App.currentAddress!!) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_edit)

        nameInput.setText(currentAddressInfo.name)
        noteInput.setText(currentAddressInfo.note)

        supportActionBar?.subtitle = getString(R.string.edit_account_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        open_on_etherscan.setOnClickListener {
            startActivityFromURL(App.networḱ.getBlockExplorer().getURLforAddress(App.currentAddress!!))
        }

        nameInput.doAfterEdit {
            currentAddressInfo.name = nameInput.text.toString()
        }

        noteInput.doAfterEdit {
            currentAddressInfo.note = noteInput.text.toString()
        }

    }

    override fun onPause() {
        super.onPause()
        addressBook.setEntry(currentAddressInfo)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
