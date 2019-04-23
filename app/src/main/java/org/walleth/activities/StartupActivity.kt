package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.ligi.kaxt.startActivityFromClass
import org.walleth.data.EXTRA_KEY_ADDRESS
import org.walleth.data.REQUEST_CODE_CREATE_ACCOUNT
import org.walleth.data.networks.CurrentAddressProvider

class StartupActivity : AppCompatActivity() {

    val currentAddressProvider: CurrentAddressProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (currentAddressProvider.getCurrent() == null) {
            startActivityForResult(Intent(this, CreateAccountActivity::class.java), REQUEST_CODE_CREATE_ACCOUNT)
        } else {
            startActivityFromClass(MainActivity::class.java)
            finish()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_CREATE_ACCOUNT -> {
                    currentAddressProvider.setCurrent(Address(data.getStringExtra(EXTRA_KEY_ADDRESS)))
                    startActivityFromClass(MainActivity::class.java)
                    finish()
                }
                else -> super.onActivityResult(requestCode, resultCode, data)
            }
        } else {
            finish()
        }
    }
}