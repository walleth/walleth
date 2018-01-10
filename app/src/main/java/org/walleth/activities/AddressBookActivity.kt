package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.walleth.R
import org.walleth.data.addressbook.AddressBookEntry

open class AddressBookActivity : BaseAddressBookActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.subtitle = getString(R.string.address_book_subtitle)
    }

    override fun onAddressClick(addressEntry: AddressBookEntry) {
        setResult(Activity.RESULT_OK, Intent().apply { putExtra("HEX", addressEntry.address.hex) })
        finish()
    }

}
