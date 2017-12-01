package org.walleth.activities

import android.arch.lifecycle.Observer
import android.os.Bundle
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_list_addresses.*
import org.walleth.R
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.ui.AddressAdapter

class SwitchAccountActivity : BaseAddressBookActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.subtitle = getString(R.string.switch_account_subtitle)
    }

    override fun setupAdapter() {

        appDatabase.addressBook.allLiveData().observe(this, Observer { items ->
            if (items != null) {
                notDeletedEntries = items.filter { !it.deleted }
                deletedEntries = items.filter { it.deleted }

                recycler_view.adapter = AddressAdapter(keyStore, {
                    currentAddressProvider.setCurrent(it.address)
                    finish()
                }, {
                    appDatabase.addressBook.upsert(it)
                }).apply {
                    updateAddressList(notDeletedEntries, starred_only.isChecked, writable_only.isChecked)
                }
            }
        })

    }

    private val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()


    override fun onResume() {
        super.onResume()
        refresh()
    }
}
