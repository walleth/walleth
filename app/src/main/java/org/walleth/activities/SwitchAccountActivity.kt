package org.walleth.activities

import android.os.Bundle
import org.walleth.R
import org.walleth.data.addressbook.AddressBookEntry

class SwitchAccountActivity : BaseAddressBookActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.subtitle = getString(R.string.switch_account_subtitle)
    }

    override fun onAddressClick(addressEntry: AddressBookEntry) {
        currentAddressProvider.setCurrent(addressEntry.address)
        finish()
    }
}
