package org.walleth.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_account_create.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.kethereum.crypto.ECKeyPair
import org.kethereum.crypto.createEcKeyPair
import org.kethereum.crypto.getAddress
import org.kethereum.erc55.withERC55Checksum
import org.kethereum.erc681.isEthereumURLString
import org.kethereum.erc681.parseERC681
import org.kethereum.functions.isValid
import org.kethereum.model.Address
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.R.string.*
import org.walleth.activities.qrscan.startScanActivityForResult
import org.walleth.activities.trezor.TrezorGetAddressActivity
import org.walleth.activities.trezor.getAddressResult
import org.walleth.activities.trezor.getPATHResult
import org.walleth.activities.trezor.hasAddressResult
import org.walleth.data.AppDatabase
import org.walleth.data.DEFAULT_PASSWORD
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.keystore.WallethKeyStore

private const val HEX_INTENT_EXTRA_KEY = "HEX"
private const val REQUEST_CODE_TREZOR = 7965

fun Context.startCreateAccountActivity(hex: String) {
    startActivity(Intent(this, CreateAccountActivity::class.java).apply {
        putExtra(HEX_INTENT_EXTRA_KEY, hex)
    })
}

class CreateAccountActivity : AppCompatActivity(), KodeinAware {
    override val kodein by closestKodein()
    private val keyStore: WallethKeyStore by instance()
    private val appDatabase: AppDatabase by instance()
    private var lastCreatedAddress: ECKeyPair? = null
    private var trezorPath: String? = null

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
                lastCreatedAddress?.let {
                    keyStore.importKey(it, DEFAULT_PASSWORD)
                }

                async(UI) {
                    async(CommonPool) {
                        appDatabase.addressBook.upsert(AddressBookEntry(
                                name = nameInput.text.toString(),
                                address = Address(hex),
                                note = noteInput.text.toString(),
                                trezorDerivationPath = trezorPath,
                                isNotificationWanted = notify_checkbox.isChecked)
                        )
                    }.await()
                    finish()
                }

            }

        }

        add_trezor.setOnClickListener {
            startActivityForResult(Intent(this, TrezorGetAddressActivity::class.java), REQUEST_CODE_TREZOR)
        }

        new_address_button.setOnClickListener {

            lastCreatedAddress = createEcKeyPair()
            lastCreatedAddress?.getAddress()?.let {
                setAddressFromExternalApplyingChecksum(it)
            }

            notify_checkbox.isChecked = true
        }

        camera_button.setOnClickListener {
            startScanActivityForResult(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        data?.run {
            getStringExtra("SCAN_RESULT")?.let { stringExtra ->
                val address = if (stringExtra.isEthereumURLString()) {
                    parseERC681(stringExtra).address
                } else {
                    stringExtra
                }
                if (address != null) {
                    setAddressFromExternalApplyingChecksum(address)
                }
            }

            if (hasAddressResult()) {
                trezorPath = getPATHResult()
                setAddressFromExternalApplyingChecksum(getAddressResult())
            }
        }
    }

    private fun setAddressFromExternalApplyingChecksum(addressHex: String) {
        if (Address(addressHex).isValid()) {
            hexInput.setText(Address(addressHex).withERC55Checksum().hex)
        } else {
            alert(getString(R.string.warning_not_a_valid_address, addressHex), getString(R.string.title_invalid_address_alert))
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
