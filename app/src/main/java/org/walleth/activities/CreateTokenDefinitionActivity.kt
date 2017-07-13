package org.walleth.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_create_token.*
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.qrscan.startScanActivityForResult
import org.walleth.data.exchangerate.TokenProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.tokens.TokenDescriptor

class CreateTokenDefinitionActivity : AppCompatActivity() {


    val tokenProvider: TokenProvider by LazyKodein(appKodein).instance()
    val networkProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_token)

        supportActionBar?.subtitle = "Create Token"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fab.setOnClickListener {
            val newDecimals = token_decimals_input.text.toString().toIntOrNull()
            val newTokenName = token_name_input.text.toString()
            val newTokenAddress = token_address_input.text.toString()
            if (newDecimals == null || newDecimals > 42) {
                alert("Please enter a valid amount of decimals")
            } else if (newTokenName.isBlank()) {
                alert("Please enter a valid name")
            } else if (newTokenAddress.isBlank()) {
                alert("Please enter a valid address")
            } else {

                tokenProvider.addToken(TokenDescriptor(
                        name = newTokenName,
                        address = newTokenAddress,
                        decimals = newDecimals
                ))

                finish()
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
