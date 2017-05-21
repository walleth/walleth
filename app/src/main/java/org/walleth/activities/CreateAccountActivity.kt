package org.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_account_create.*
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.R.string.*
import org.walleth.data.WallethAddress
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.keystore.WallethKeyStore

class CreateAccountActivity : AppCompatActivity() {

    val addressBook: AddressBook by LazyKodein(appKodein).instance()
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_create)

        supportActionBar?.subtitle = getString(create_account_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fab.setOnClickListener {
            val hex = hexInput.text.toString()

            if (!hex.startsWith("0x")) {
                alert(title = alert_problem_title, message = address_not_valid)
            } else {
                addressBook.setEntry(AddressBookEntry(nameInput.text.toString(), WallethAddress(hex), noteInput.text.toString()))
                finish()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
