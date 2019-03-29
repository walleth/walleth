package org.walleth.activities

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_create_token.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.qrscan.startScanActivityForResult
import org.walleth.data.AppDatabase
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.tokens.Token


class CreateTokenDefinitionActivity : BaseSubActivity() {


    val appDatabase: AppDatabase by inject()
    val networkDefinitionProvider: NetworkDefinitionProvider by inject()

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
                networkDefinitionProvider.observe(this, Observer { networkDefinition ->
                    if (networkDefinition == null)
                        throw IllegalStateException("NetworkDefinition should not be null")

                    GlobalScope.async(Dispatchers.Main) {
                        async(Dispatchers.Default) {
                            appDatabase.tokens.upsert(Token(
                                    name = newTokenName,
                                    symbol = newTokenName,
                                    address = Address(newTokenAddress),
                                    decimals = newDecimals,
                                    chain = networkDefinition.chain.id.value,
                                    showInList = true,
                                    starred = true,
                                    fromUser = true,
                                    order = 0
                            ))
                        }.await()
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
