package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxtui.alert
import org.walleth.data.EXTRA_KEY_ADDRESS
import org.walleth.data.REQUEST_CODE_CREATE_ACCOUNT
import org.walleth.data.networks.ChainInfoProvider
import org.walleth.data.networks.CurrentAddressProvider

class StartupActivity : AppCompatActivity() {

    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val currentChainProvider: ChainInfoProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkEnvironment(false)
    }

    private fun checkEnvironment(failForChain: Boolean) {
        when {
            currentAddressProvider.getCurrent() == null -> startActivityForResult(Intent(this, CreateAccountActivity::class.java), REQUEST_CODE_CREATE_ACCOUNT)
            currentChainProvider.getCurrent() == null -> if (failForChain) {
                alert("chain must not be null") {
                    Handler().postDelayed({
                        checkEnvironment(true)
                    }, 700)
                }
            } else {
                checkEnvironment(true)
            }
            else -> {
                startActivityFromClass(MainActivity::class.java)
                finish()
            }
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