package org.walleth.activities

import android.arch.lifecycle.Observer
import android.os.Bundle
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_list.*
import org.walleth.R
import org.walleth.data.networks.BaseCurrentAddressProvider
import org.walleth.ui.AddressAdapter

class SwitchAccountActivity : BaseAddressBookActivity() {

    private val currentAddressProvider: BaseCurrentAddressProvider by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = getString(R.string.switch_account_subtitle)
    }

    override fun onResume() {
        super.onResume()

        appDatabase.addressBook.allLiveData().observe(this, Observer { lol2 ->
            recycler_view.adapter = AddressAdapter(lol2!!, keyStore) {
                currentAddressProvider.setCurrent(it.address)
                finish()
            }
        })


    }
}
