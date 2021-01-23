package org.walleth.accounts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_account_create.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.crypto.createEthereumKeyPair
import org.kethereum.crypto.toAddress
import org.kethereum.eip137.model.ENSName
import org.kethereum.ens.isPotentialENSDomain
import org.kethereum.erc55.isValid
import org.kethereum.erc55.withERC55Checksum
import org.kethereum.erc681.parseERC681
import org.kethereum.erc831.isEthereumURLString
import org.kethereum.keystore.api.KeyStore
import org.kethereum.model.Address
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PrivateKey
import org.kethereum.model.PublicKey
import org.koin.android.ext.android.inject
import org.komputing.khex.model.HexString
import org.ligi.kaxt.setVisibility
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.*
import org.walleth.data.addresses.AccountKeySpec
import org.walleth.data.addresses.AddressBookEntry
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.addresses.toJSON
import org.walleth.data.ens.ENSProvider
import org.walleth.trezor.getAddressResult
import org.walleth.trezor.hasAddressResult
import org.walleth.util.hasText

fun Context.startCreateAccountActivity(hex: String) {
    startActivity(Intent(this, CreateAccountActivity::class.java).apply {
        putExtra(EXTRA_KEY_ACCOUNTSPEC, AccountKeySpec(ACCOUNT_TYPE_WATCH_ONLY, source = hex))
    })
}

class CreateAccountActivity : BaseSubActivity() {

    private val keyStore: KeyStore by inject()
    private val appDatabase: AppDatabase by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val ensProvider: ENSProvider by inject()

    private var currentSpec: AccountKeySpec = AccountKeySpec(ACCOUNT_TYPE_NONE)
    private var currentAddress: Address? = null
    private var isCreatingAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_create)

        supportActionBar?.subtitle = getString(R.string.create_account_subtitle)

        intent.getParcelableExtra<AccountKeySpec>(EXTRA_KEY_ACCOUNTSPEC)?.let {
            currentSpec = it
        }

        when (currentSpec.type) {
            ACCOUNT_TYPE_WATCH_ONLY -> currentSpec.source?.let {
                setAddressFromExternalApplyingChecksum(Address(it))
            }
            ACCOUNT_TYPE_IMPORT -> startActivityForResult(getKeyImportIntent(currentSpec), REQUEST_CODE_IMPORT)
            ACCOUNT_TYPE_NONE -> selectAccountType()
        }

        type_select_button.setOnClickListener {
            selectAccountTypeNoScanOption()
        }

        fab.setOnClickListener {
            if (!isCreatingAccount) { // prevent problems by multi-clicking on FAB
                if (!nameInput.hasText()) {
                    if (currentSpec.type != ACCOUNT_TYPE_BURNER) {
                        nameInput.error = getString(R.string.please_enter_name)
                        nameInput.requestFocus()
                        return@setOnClickListener
                    } else {
                        nameInput.setText(R.string.burner_label)
                    }
                }
                val importKey = currentSpec.initPayload?.let {
                    val split = it.split("/")
                    val privateKey = PrivateKey(HexString(split.first()))
                    val publicKey = PublicKey(HexString(split.last()))
                    ECKeyPair(privateKey, publicKey)
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
                    ACCOUNT_TYPE_NFC, ACCOUNT_TYPE_TREZOR, ACCOUNT_TYPE_KEEPKEY -> {
                        if (currentAddress == null) {
                            alert("This should not happen - please drop a mail to walleth@walleth.org and let us know when this happened")
                        } else {
                            createAccountAndFinish(currentAddress!!, currentSpec)
                        }
                    }

                    ACCOUNT_TYPE_WATCH_ONLY -> {

                        val potentialENSName = ENSName(input_address.text.toString())
                        if (potentialENSName.isPotentialENSDomain()) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val address = ensProvider.get()?.getAddress(potentialENSName)
                                if (address != null) {
                                    createAccountAndFinish(address, currentSpec)
                                } else {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        alert("Cannot find this ENS address")
                                    }
                                }

                            }

                        } else {

                            val candidate = Address(input_address.text.toString())
                            if (candidate.isValid()) {
                                createAccountAndFinish(candidate, currentSpec)
                            } else {
                                input_address.error = getString(R.string.title_invalid_address_alert)
                                input_address.requestFocus()
                            }
                        }

                    }
                }
            }


        }

        applyViewModel()
    }

    private fun createAccountAndFinish(address: Address, keySpec: AccountKeySpec) {
        isCreatingAccount = true
        lifecycleScope.launch(Dispatchers.Main) {
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
        startActivityForResult(getNewAccountTypeSelectActivityIntent(), REQUEST_CODE_PICK_ACCOUNT_TYPE)
    }

    private fun selectAccountTypeNoScanOption() {
        startActivityForResult(getNewAccountTypeSelectActivityNoFABIntent(), REQUEST_CODE_PICK_ACCOUNT_TYPE)
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

        currentSpec.name?.let {
            nameInput.setText(it)
        }

        val accountType = ACCOUNT_TYPE_MAP[currentSpec.type]
        type_image.setImageResource(accountType?.drawable ?: R.drawable.ic_warning_black_24dp)

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            if (currentAddressProvider.getCurrent() != null) {
                finish()
            }
            return
        }

        if (data.hasAddressResult()) {
            setAddressFromExternalApplyingChecksum(Address(data.getAddressResult()!!))
        }
        data?.run {
            when (requestCode) {
                REQUEST_CODE_PICK_ACCOUNT_TYPE, REQUEST_CODE_IMPORT -> {
                    currentSpec = data.getParcelableExtra(EXTRA_KEY_ACCOUNTSPEC) ?: throw(IllegalStateException("EXTRA_KEY_ACCOUNTSPEC was not passed"))
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
