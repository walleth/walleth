package org.walleth.activities

import android.os.Bundle
import android.view.View.GONE
import kotlinx.android.synthetic.main.activity_list.*
import org.walleth.R
import org.walleth.ui.AddressAdapter

class SwitchAccountActivity : BaseAddressBookActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = getString(R.string.switch_account_subtitle)
        fab.visibility = GONE
    }

    override fun getAdapter() = AddressAdapter(addressBook.getAllEntries()) {
        keyStore.setCurrentAddress(it.address)
        finish()
    }


}
