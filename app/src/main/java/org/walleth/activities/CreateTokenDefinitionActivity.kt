package org.walleth.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_create_token.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.networks.ChainInfoProvider
import org.walleth.data.tokens.Token
import org.walleth.qrscan.startScanActivityForResult


class CreateTokenDefinitionActivity : BaseSubActivity() {


    val appDatabase: AppDatabase by inject()
    val chainInfoProvider: ChainInfoProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_token)

        supportActionBar?.subtitle = getString(R.string.create_token_activity_subtitle)

        fab.setOnClickListener {
            val newDecimals = token_decimals_input.text.toString().toIntOrNull()
            val newTokenName = token_name_input.text.toString()
            val newTokenAddress = token_address_input.text.toString()
            if (newDecimals == null || newDecimals > 42) {
                alert(R.string.create_token_activity_error_invalid_amount_of_decimals)
            } else if (newTokenName.isBlank()) {
                alert(R.string.create_token_activity_error_invalid_name)
            } else if (newTokenAddress.isBlank()) {
                alert(R.string.create_token_activity_error_invalid_address)
            } else {
                chainInfoProvider.observe(this, Observer { networkDefinition ->
                    if (networkDefinition == null)
                        throw IllegalStateException("NetworkDefinition should not be null")

                    GlobalScope.async(Dispatchers.Main) {
                        withContext(Dispatchers.Default) {
                            appDatabase.tokens.upsert(Token(
                                    name = newTokenName,
                                    symbol = newTokenName,
                                    address = Address(newTokenAddress),
                                    decimals = newDecimals,
                                    chain = networkDefinition.chainId,
                                    softDeleted = false,
                                    starred = true,
                                    fromUser = true,
                                    order = 0
                            ))
                        }
                        finish()
                    }
                })

            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_import, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.let {
            if (data.hasExtra("SCAN_RESULT")) {
                token_address_input.setText(data.getStringExtra("SCAN_RESULT"))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_scan -> true.also {
            startScanActivityForResult(this)
        }
        else -> super.onOptionsItemSelected(item)
    }
}
