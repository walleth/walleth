package org.walleth.activities

import android.os.Bundle
import android.support.v7.app.AlertDialog
import org.koin.android.ext.android.inject
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider

class KeysActivity : BaseSubActivity() {

    val keyStore: WallethKeyStore by inject()
    val currentAddressProvider: CurrentAddressProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!keyStore.hasKeyForForAddress(currentAddressProvider.getCurrent())) {
            startActivityFromClass(ImportActivity::class.java)
            finish()
        } else {
            val items = arrayOf(getString(R.string.nav_drawer_import_key), getString(R.string.nav_drawer_export_key))
            AlertDialog.Builder(this)
                    .setOnCancelListener {
                        finish()
                    }
                    .setItems(items) { _, i ->
                        when (i) {
                            0 -> startActivityFromClass(ImportActivity::class.java)
                            else -> startActivityFromClass(ExportKeyActivity::class.java)
                        }
                        finish()
                    }
                    .show()
        }


    }

}
