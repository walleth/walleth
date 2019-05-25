package org.walleth.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
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
import org.kethereum.model.PrivateKey
import org.kethereum.model.PublicKey
import org.koin.android.ext.android.inject
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.setVisibility
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.trezor.getAddressResult
import org.walleth.activities.trezor.hasAddressResult
import org.walleth.data.*
import org.walleth.data.addressbook.AccountKeySpec
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.addressbook.toJSON
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.model.ACCOUNT_TYPE_MAP
import org.walleth.util.hasText

private const val HEX_INTENT_EXTRA_KEY = "HEX"

fun Context.startCreateAccountActivity(hex: String) {
    startActivity(Intent(this, CreateAccountActivity::class.java).apply {
        putExtra(HEX_INTENT_EXTRA_KEY, hex)
    })
}

class CreateAccountActivity : BaseSubActivity() {

    private val keyStore: KeyStore by inject()
    private val appDatabase: AppDatabase by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()

    private var currentSpec: AccountKeySpec = AccountKeySpec(ACCOUNT_TYPE_NONE)
    private var currentAddress: Address? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_create)

        supportActionBar?.subtitle = getString(R.string.create_account_subtitle)

        intent.getStringExtra(HEX_INTENT_EXTRA_KEY)?.let {
            currentSpec = AccountKeySpec(ACCOUNT_TYPE_WATCH_ONLY)
            setAddressFromExternalApplyingChecksum(Address(it))
        }

        if (currentSpec.type == ACCOUNT_TYPE_NONE) {
            startActivityForResult(Intent(this, NewAccountTypeSelectActivity::class.java), REQUEST_CODE_PICK_ACCOUNT_TYPE)
        }

        type_select_button.setOnClickListener {
            startActivityForResult(Intent(this, NewAccountTypeSelectActivity::class.java), REQUEST_CODE_PICK_ACCOUNT_TYPE)
        }

        input_address.doAfterEdit {
            val candidate = Address(it.toString())
            if (candidate.isValid()) {
                setAddressFromExternalApplyingChecksum(candidate)
            }

        }

        fab.setOnClickListener {
            if (!nameInput.hasText()) {
                alert(title = R.string.alert_problem_title, message = R.string.please_enter_name)
                return@setOnClickListener
            }
            val importKey = currentSpec.initPayload?.let {
                val split = it.split("/")
                ECKeyPair(PrivateKey(split.first()), PublicKey(split.last()))
            }
            when (currentSpec.type) {

                ACCOUNT_TYPE_BURNER -> {
                    val key = importKey ?: createEthereumKeyPair()
                    keyStore.addKey(key, DEFAULT_PASSWORD, true)

                    createAccountAndFinish(key.toAddress(), currentSpec)

                }

                ACCOUNT_TYPE_PIN_PROTECTED, ACCOUNT_TYPE_PASSWORD_PROTECTED -> {
                    val key = importKey ?: createEthereumKeyPair()
                    keyStore.addKey(key, currentSpec.pwd!!, true)

                    createAccountAndFinish(key.toAddress(), currentSpec.copy(pwd = null))
                }
                ACCOUNT_TYPE_NFC, ACCOUNT_TYPE_TREZOR, ACCOUNT_TYPE_WATCH_ONLY -> {
                    if (currentAddress == null) {
                        alert("Invalid address")
                    } else {
                        createAccountAndFinish(currentAddress!!, currentSpec)
                    }
                }
            }


        }

        applyViewModel()
    }

    private fun createAccountAndFinish(address: Address, keySpec: AccountKeySpec) {
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                appDatabase.addressBook.upsert(AddressBookEntry(
                        name = nameInput.text.toString(),
                        address = address,
                        note = noteInput.text.toString(),
                        keySpec = keySpec.toJSON(),
                        isNotificationWanted = notify_checkbox.isChecked)
                )
            }
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_KEY_ADDRESS, address.hex))
            finish()
        }
    }

    override fun onBackPressed() {
        if (currentAddressProvider.getCurrent() == null) {
            selectAccountType()
        }
    }

    private fun selectAccountType() {
        val newAccountTypeSelectActivity = Intent(this, NewAccountTypeSelectActivity::class.java)
        startActivityForResult(newAccountTypeSelectActivity, REQUEST_CODE_PICK_ACCOUNT_TYPE)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> false.also { selectAccountType() }
        else -> true
    }

    private fun applyViewModel() {
        val noneSelected = currentSpec.type == ACCOUNT_TYPE_NONE
        type_image.setVisibility(!noneSelected)
        type_select_button.text = if (noneSelected) "select" else "switch"

        input_address_layout.setVisibility(currentSpec.type == ACCOUNT_TYPE_WATCH_ONLY)
        input_address.setText(currentAddress?.hex)

        val accountType = ACCOUNT_TYPE_MAP[currentSpec.type]
        type_image.setImageResource(accountType?.drawable ?: R.drawable.ic_warning_black_24dp)

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        if (data.hasAddressResult()) {
            setAddressFromExternalApplyingChecksum(Address(data.getAddressResult()!!))
        }
        data?.run {
            when (requestCode) {
                REQUEST_CODE_PICK_ACCOUNT_TYPE -> {
                    currentSpec = data.getParcelableExtra(EXTRA_KEY_ACCOUNTSPEC)
                    applyViewModel()
                }
                else -> {
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
                }
            }

        }
    }

    private fun setAddressFromExternalApplyingChecksum(address: Address) {
        if (address.isValid()) {
            currentAddress = address.withERC55Checksum()
        } else {
            alert(getString(R.string.warning_not_a_valid_address, address), getString(R.string.title_invalid_address_alert))
        }
    }
}
