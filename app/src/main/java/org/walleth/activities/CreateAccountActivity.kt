package org.walleth.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.FEATURE_USB_HOST
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_account_create.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.crypto.createEthereumKeyPair
import org.kethereum.crypto.toAddress
import org.kethereum.erc55.withERC55Checksum
import org.kethereum.erc681.parseERC681
import org.kethereum.erc831.isEthereumURLString
import org.kethereum.functions.isValid
import org.kethereum.keystore.api.KeyStore
import org.kethereum.model.Address
import org.kethereum.model.ECKeyPair
import org.koin.android.ext.android.inject
import org.ligi.kaxt.setVisibility
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
import org.walleth.util.hasText

private const val HEX_INTENT_EXTRA_KEY = "HEX"
private const val REQUEST_CODE_TREZOR = 7965

fun Context.startCreateAccountActivity(hex: String) {
    startActivity(Intent(this, CreateAccountActivity::class.java).apply {
        putExtra(HEX_INTENT_EXTRA_KEY, hex)
    })
}

class CreateAccountActivity : BaseSubActivity() {

    private val keyStore: KeyStore by inject()
    private val appDatabase: AppDatabase by inject()
    private var lastCreatedAddress: ECKeyPair? = null
    private var trezorPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_create)

        supportActionBar?.subtitle = getString(R.string.create_account_subtitle)

        intent.getStringExtra(HEX_INTENT_EXTRA_KEY)?.let {
            hexInput.setText(it)
        }

        fab.setOnClickListener {
            val hex = hexInput.text.toString()

            if (!Address(hex).isValid()) {
                alert(title = alert_problem_title, message = address_not_valid)
            } else if (!nameInput.hasText()) {
                alert(title = alert_problem_title, message = please_enter_name)
            } else {
                lastCreatedAddress?.let {
                    keyStore.addKey(it, DEFAULT_PASSWORD, true)
                }

                GlobalScope.launch(Dispatchers.Main) {
                    withContext(Dispatchers.Default) {
                        appDatabase.addressBook.upsert(AddressBookEntry(
                                name = nameInput.text.toString(),
                                address = Address(hex),
                                note = noteInput.text.toString(),
                                trezorDerivationPath = trezorPath,
                                isNotificationWanted = notify_checkbox.isChecked)
                        )
                    }
                    finish()
                }

            }

        }

        add_trezor.setVisibility(packageManager.hasSystemFeature(FEATURE_USB_HOST))
        add_trezor.setOnClickListener {
            startActivityForResult(Intent(this, TrezorGetAddressActivity::class.java), REQUEST_CODE_TREZOR)
        }

        new_address_button.setOnClickListener {

            lastCreatedAddress = createEthereumKeyPair()
            lastCreatedAddress?.toAddress()?.let {
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
                    setAddressFromExternalApplyingChecksum(Address(address))
                }
            }

            if (hasAddressResult()) {
                trezorPath = getPATHResult()
                setAddressFromExternalApplyingChecksum(Address(getAddressResult()))
            }
        }
    }

    private fun setAddressFromExternalApplyingChecksum(addressHex: Address) {
        if (addressHex.isValid()) {
            hexInput.setText(addressHex.withERC55Checksum().hex)
        } else {
            alert(getString(R.string.warning_not_a_valid_address, addressHex), getString(R.string.title_invalid_address_alert))
        }
    }
}
