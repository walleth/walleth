package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.walleth.R
import org.walleth.ui.AddressAdapter

open class AddressBookActivity : BaseAddressBookActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = getString(R.string.address_book_subtitle)

        async(UI) {
            val allEntries = async(CommonPool) {
                appDatabase.addressBook.all()
            }.await()
            recycler_view.adapter = AddressAdapter(allEntries, keyStore) {
                setResult(Activity.RESULT_OK, Intent().apply { putExtra("HEX", it.address.hex) })
                finish()
            }

        }

    }

}
