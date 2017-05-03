package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.walleth.R
import org.walleth.ui.AddressAdapter

open class AddressBookActivity : BaseAddressBookActivity() {

    override fun getAdapter() = AddressAdapter(addressBook.getAllEntries()) {
        setResult(Activity.RESULT_OK, Intent().apply { putExtra("HEX", it.address.hex) })
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = getString(R.string.edit_account_subtitle)
    }

}
