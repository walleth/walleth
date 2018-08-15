package org.walleth.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.activity_wallet_connect.*
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.withContext
import org.kethereum.model.Address
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.walletconnect.model.Session
import org.walleth.walletconnect.model.StatefulWalletConnectTransaction
import org.walleth.walletconnect.WalletConnectDriver
import org.walleth.walletconnect.createIntentForTransaction

private const val KEY_INTENT_JSON = "JSON_KEY"

fun Context.getWalletConnectIntent(json: String) = Intent(this, WalletConnectConnectionActivity::class.java).apply {
    putExtra(KEY_INTENT_JSON, json)
}

class WalletConnectConnectionActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val moshi: Moshi by instance()
    private val walletConnectDriver: WalletConnectDriver by instance()
    private val currentAddressProvider: CurrentAddressProvider by instance()

    private var currentTransaction: StatefulWalletConnectTransaction? = null

    private val currentSession by lazy {
        intent.getStringExtra(KEY_INTENT_JSON)?.let {
            moshi.adapter(Session::class.java).fromJson(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wallet_connect)
        supportActionBar?.subtitle = getString(R.string.wallet_connect) + currentSession?.dappName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        status_text.text = "Connecting to ${currentSession?.dappName}"
        currentSession?.let {

            AlertDialog.Builder(this)
                    .setTitle("Do you want to use ${currentSession?.dappName}")
                    .setItems(arrayOf("Current Account", "Select Account", "Abort")) { _, i ->
                        when (i) {
                            0 -> start(currentAddressProvider.getCurrent())

                            1 -> {
                                val intent = Intent(this, AddressBookActivity::class.java)
                                startActivityForResult(intent, TO_ADDRESS_REQUEST_CODE)
                            }
                            else -> finish()
                        }
                    }
                    .show()


        }
    }

    override fun onResume() {
        super.onResume()
        walletConnectDriver.txAction = processTX
    }


    override fun onPause() {
        super.onPause()
        walletConnectDriver.txAction = null
    }

    private fun start(address: Address) {
        currentSession?.let {
            async(UI) {
                val result = try {
                    val response = withContext(DefaultDispatcher) {
                        walletConnectDriver.sendAddress(it, address)
                     }
                    response?.code()
                } catch (e: Exception) {
                    e.printStackTrace()
                    e.message
                }
                if (result == 200) {
                    status_text.text = "Waiting for interaction on ${currentSession?.dappName}"
                } else {
                    alert("There was a problem connection to ${currentSession?.dappName}") {
                        finish()
                    }
                }
            }
        }
    }

    private val processTX: (StatefulWalletConnectTransaction) -> Unit = { statefulWalletConnectTransaction ->
        currentTransaction = statefulWalletConnectTransaction
        startActivity(createIntentForTransaction(statefulWalletConnectTransaction))
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        data?.let {
            when (requestCode) {
                FROM_ADDRESS_REQUEST_CODE -> {
                    if (data.hasExtra("HEX")) {
                        start(Address(data.getStringExtra("HEX")))
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> true.also {
            finish()
        }
        else -> super.onOptionsItemSelected(item)
    }
}
