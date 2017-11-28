package org.walleth.activities

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_create_token.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.kethereum.model.Address
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.qrscan.startScanActivityForResult
import org.walleth.data.AppDatabase
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.tokens.Token


class CreateTokenDefinitionActivity : AppCompatActivity() {

    val appDatabase: AppDatabase by LazyKodein(appKodein).instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_token)

        supportActionBar?.subtitle = getString(R.string.create_token_activity_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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

                    async(UI) {
                        async(CommonPool) {
                            appDatabase.tokens.upsert(Token(
                                    name = newTokenName,
                                    symbol = newTokenName,
                                    address = Address(newTokenAddress),
                                    decimals = newDecimals,
                                    chain = networkDefinition.chain,
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
        android.R.id.home -> {
            finish()
            true
        }
        R.id.menu_scan -> {
            startScanActivityForResult(this)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
