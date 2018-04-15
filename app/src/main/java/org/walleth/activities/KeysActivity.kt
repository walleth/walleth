package org.walleth.activities

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider


class KeysActivity : AppCompatActivity() {

    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!keyStore.hasKeyForForAddress(currentAddressProvider.getCurrent())) {
            startActivityFromClass(ImportActivity::class.java)
            finish()
        } else {
            val items = arrayOf(getString(R.string.nav_drawer_import_key),getString(R.string.nav_drawer_export_key))
            AlertDialog.Builder(this)
                    .setOnCancelListener {
                        finish()
                    }
                    .setItems(items, { _, i ->
                        startActivityFromClass(when (i) {
                            0 -> ImportActivity::class.java
                            else -> ExportKeyActivity::class.java
                        })
                        finish()
                    })
                    .show()
        }


    }

}
