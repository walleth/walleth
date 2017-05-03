package org.walleth.activities

import android.os.Bundle
import org.walleth.R
import org.walleth.ui.AddressAdapter

class SwitchAccountActivity : BaseAddressBookActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = getString(R.string.edit_account_subtitle)
    }

    override fun getAdapter() = AddressAdapter(addressBook.getAllEntries()) {
        keyStore.setCurrentAddress(it.address)
        finish()
    }


}
