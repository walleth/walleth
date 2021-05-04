package org.walleth.accounts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.walleth.R
import org.walleth.data.EXTRA_KEY_ADDRESS
import org.walleth.data.addresses.AddressBookEntry

class AccountPickActivity : BaseAddressBookActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.subtitle = getString(R.string.address_book_subtitle)
    }

    override fun getPostCreateActionLabelResId() = R.string.post_account_create_action_label_pick

    override fun onAddressClick(addressEntry: AddressBookEntry) {
        setResult(Activity.RESULT_OK, Intent().apply { putExtra(EXTRA_KEY_ADDRESS, addressEntry.address.hex) })
        finish()
    }

}
