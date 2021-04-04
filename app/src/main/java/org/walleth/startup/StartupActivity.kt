package org.walleth.startup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxtui.alert
import org.walleth.accounts.CreateAccountActivity
import org.walleth.data.EXTRA_KEY_ADDRESS
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.overview.OverviewActivity
import org.walleth.startup.StartupStatus.*

class StartupActivity : AppCompatActivity() {

    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val viewModel: StartupViewModel by viewModel()

    private val createAccountActionForResult = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK && it.data != null) {

            val addressString = it.data?.getStringExtra(EXTRA_KEY_ADDRESS) ?: throw IllegalStateException("No EXTRA_KEY_ADDRESS in onActivityResult")
            val address = Address(addressString)

            lifecycleScope.launch(Dispatchers.Main) {
                currentAddressProvider.setCurrent(address)
                startActivityFromClass(OverviewActivity::class.java)
                finish()
            }

        } else {
            finish()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.status.observe(this, Observer<StartupStatus> { status ->
            when (status) {
                is NeedsAddress -> {
                    createAccountActionForResult.launch(Intent(this, CreateAccountActivity::class.java))
                }
                is HasChainAndAddress -> {
                    startActivityFromClass(OverviewActivity::class.java)
                    finish()
                }
                is Timeout -> {
                    alert("chain must not be null - this should never happen - please let walleth@walleth.org if you see this. Thanks and sorry for the noise.") {
                        finish()
                    }
                }
            }
        })
    }
}