package org.walleth.activities

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import org.walleth.R
import org.walleth.data.addressbook.AddressBookEntry

class SwitchAccountActivity : BaseAddressBookActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.subtitle = getString(R.string.nav_drawer_accounts)
    }

    override fun onAddressClick(addressEntry: AddressBookEntry) {
        currentAddressProvider.setCurrent(addressEntry.address)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            menuInflater.inflate(R.menu.menu_address_transfer, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_export -> true.also { startActivity(getExportAccountsIntent()) }
        R.id.menu_import -> true.also { startActivity(getImportAccountsIntent()) }
        else -> super.onOptionsItemSelected(item)
    }
}
