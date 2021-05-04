package org.walleth.accounts

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.walleth.R
import org.walleth.data.addresses.AddressBookEntry

class SwitchAccountActivity : BaseAddressBookActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.subtitle = getString(R.string.nav_drawer_accounts)
    }

    override fun getPostCreateActionLabelResId() = R.string.post_account_create_action_label_switch

    override fun onAddressClick(addressEntry: AddressBookEntry) {
        lifecycleScope.launch(Dispatchers.Main) {
            currentAddressProvider.setCurrent(addressEntry.address)
            finish()
        }
    }
}
