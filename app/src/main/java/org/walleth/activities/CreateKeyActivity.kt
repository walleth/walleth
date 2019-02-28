package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kethereum.crypto.createEthereumKeyPair
import org.kethereum.keystore.api.KeyStore
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.ligi.kaxt.livedata.nonNull
import org.ligi.kaxt.livedata.observe
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.DEFAULT_PASSWORD
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.networks.CurrentAddressProvider

class CreateKeyActivity : AppCompatActivity() {
    private val keyStore: KeyStore by inject()
    private val appDatabase: AppDatabase by inject()
    val currentAddressProvider: CurrentAddressProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.creating_key_busy_indicator)
        setSupportActionBar(toolbar)
        GlobalScope.launch {
            val newAccountAddress = keyStore.addKey(createEthereumKeyPair(), DEFAULT_PASSWORD)
                    ?: throw (IllegalArgumentException("Could not create key"))


            appDatabase.addressBook.upsert(AddressBookEntry(
                    name = getString(R.string.default_account_name),
                    address = newAccountAddress,
                    note = getString(R.string.new_address_note),
                    isNotificationWanted = true,
                    trezorDerivationPath = null
            ))
            currentAddressProvider.postValue(newAccountAddress)
        }

        currentAddressProvider.nonNull().observe(this) {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            currentAddressProvider.setCurrent(Address(data.getStringExtra("ADDRESS")))
        }
    }
}