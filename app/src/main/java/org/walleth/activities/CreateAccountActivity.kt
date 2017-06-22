package org.walleth.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_account_create.*
import org.kethereum.functions.isValid
import org.kethereum.model.Address
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.R.string.*
import org.walleth.data.DEFAULT_PASSWORD
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.iac.BarCodeIntentIntegrator
import org.walleth.iac.BarCodeIntentIntegrator.QR_CODE_TYPES
import org.walleth.iac.ERC67
import org.walleth.iac.isERC67String

private val HEX_INTENT_EXTRA_KEY = "HEX"
fun Context.startCreateAccountActivity(hex: String) {
    startActivity(Intent(this, CreateAccountActivity::class.java).apply {
        putExtra(HEX_INTENT_EXTRA_KEY, hex)
    })
}

class CreateAccountActivity : AppCompatActivity() {

    val addressBook: AddressBook by LazyKodein(appKodein).instance()
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    var lastCreatedAddress: Address? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_create)

        supportActionBar?.subtitle = getString(create_account_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        intent.getStringExtra(HEX_INTENT_EXTRA_KEY)?.let {
            hexInput.setText(it)
        }

        fab.setOnClickListener {
            val hex = hexInput.text.toString()

            if (!Address(hex).isValid()) {
                alert(title = alert_problem_title, message = address_not_valid)
            } else if (nameInput.text.isBlank()) {
                alert(title = alert_problem_title, message = please_enter_name)
            } else {
                lastCreatedAddress = null // prevent cleanup
                addressBook.setEntry(AddressBookEntry(
                        name = nameInput.text.toString(),
                        address = Address(hex),
                        note = noteInput.text.toString(),
                        isNotificationWanted = notify_checkbox.isChecked)
                )
                finish()
            }
        }
        new_address_button.setOnClickListener {
            cleanupGeneratedKeyWhenNeeded()
            val newAddress = keyStore.newAddress(DEFAULT_PASSWORD)
            lastCreatedAddress = newAddress
            hexInput.setText(newAddress.hex)
            notify_checkbox.isChecked = true
        }

        camera_button.setOnClickListener {
            BarCodeIntentIntegrator(this).initiateScan(QR_CODE_TYPES)
        }
    }

    private fun cleanupGeneratedKeyWhenNeeded() {
        lastCreatedAddress?.let {
            keyStore.deleteKey(it, DEFAULT_PASSWORD)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && data.hasExtra("SCAN_RESULT")) {
            hexInput.setText(if (!data.getStringExtra("SCAN_RESULT").isERC67String()) {
                data.getStringExtra("SCAN_RESULT")
            } else {
                ERC67(data.getStringExtra("SCAN_RESULT")).getHex()
            })
        }
    }

    override fun onPause() {
        super.onPause()
        cleanupGeneratedKeyWhenNeeded()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
