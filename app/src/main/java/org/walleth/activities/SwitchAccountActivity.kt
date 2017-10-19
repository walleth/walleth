package org.walleth.activities

import android.arch.lifecycle.Observer
import android.os.Bundle
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_list.*
import org.walleth.R
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.ui.AddressAdapter

class SwitchAccountActivity : BaseAddressBookActivity() {
    override fun setupAdapter() {

        appDatabase.addressBook.allLiveData().observe(this, Observer { items ->
            if (items != null) {
                notDeletedEntries = items.filter { !it.deleted }
                deletedEntries = items.filter { it.deleted }

                recycler_view.adapter = AddressAdapter(notDeletedEntries, keyStore) {
                    currentAddressProvider.setCurrent(it.address)
                    finish()
                }
            }
        })

    }

    private val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = getString(R.string.switch_account_subtitle)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }
}
