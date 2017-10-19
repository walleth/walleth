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
    }

    override fun setupAdapter() {
        async(UI) {
             async(CommonPool) {
                 val all = appDatabase.addressBook.all()
                 notDeletedEntries = all.filter { !it.deleted }
                 deletedEntries = all.filter { it.deleted }
            }.await()

            recycler_view.adapter = AddressAdapter(notDeletedEntries, keyStore) {
                setResult(Activity.RESULT_OK, Intent().apply { putExtra("HEX", it.address.hex) })
                finish()
            }

        }
    }

}
