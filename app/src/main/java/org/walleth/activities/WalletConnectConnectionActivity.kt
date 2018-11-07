package org.walleth.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Base64
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.activity_wallet_connect.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.kethereum.erc1328.ERC1328
import org.kethereum.erc1328.isERC1328
import org.kethereum.erc1328.toERC1328
import org.kethereum.model.Address
import org.kethereum.model.EthereumURI
import org.kodein.di.generic.instance
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.khex.toHexString
import org.walleth.walletconnect.WalletConnectDriver
import org.walleth.walletconnect.createIntentForTransaction
import org.walleth.walletconnect.model.Session
import org.walleth.walletconnect.model.StatefulWalletConnectTransaction
import java.net.URLDecoder

private const val KEY_INTENT_JSON = "JSON_KEY"

fun Context.getWalletConnectIntent(json: String) = Intent(this, WalletConnectConnectionActivity::class.java).apply {
    putExtra(KEY_INTENT_JSON, json)
}

fun ERC1328.toSession() = Session(
        sessionId = sessionID!!,
        domain = URLDecoder.decode(bridge!!, "utf-8"),
        dappName = URLDecoder.decode(name!!, "utf-8"),
        sharedKey = Base64.decode(symKey!!, Base64.DEFAULT).toHexString()
)

class WalletConnectConnectionActivity : BaseSubActivity() {

    private val moshi: Moshi by instance()
    private val walletConnectDriver: WalletConnectDriver by instance()
    private val currentAddressProvider: CurrentAddressProvider by instance()

    private var currentTransaction: StatefulWalletConnectTransaction? = null

    private val currentSession by lazy {
        val erc831 = intent.data?.let { EthereumURI(it.toString()) }
        if (erc831?.isERC1328() == true) {
            erc831.toERC1328().toSession()
        } else intent.getStringExtra(KEY_INTENT_JSON)?.let {
            moshi.adapter(Session::class.java).fromJson(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wallet_connect)
        supportActionBar?.subtitle = getString(R.string.wallet_connect) + " " + currentSession?.dappName

        if (!walletConnectDriver.hasFCMToken()) {
            alert(R.string.walletconnect_error_needs_fcm_message, R.string.walletconnect_error_needs_fcm_title) {
                finish()
            }
            return
        }
        status_text.text = getString(R.string.walletconnect_connecting_to, currentSession?.dappName)
        currentSession?.let {

            AlertDialog.Builder(this)
                    .setTitle(getString(R.string.walletconnect_do_you_want_to_use, currentSession?.dappName))
                    .setItems(R.array.walletconnect_options) { _, i ->
                        when (i) {
                            0 -> start(currentAddressProvider.getCurrent())

                            1 -> {
                                val intent = Intent(this, AddressBookActivity::class.java)
                                startActivityForResult(intent, TO_ADDRESS_REQUEST_CODE)
                            }
                            else -> finish()
                        }
                    }
                    .setOnCancelListener { _ -> finish() }
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
            GlobalScope.async (Dispatchers.Main) {
                val result = try {
                    val response = withContext(Dispatchers.Default) {
                        walletConnectDriver.sendAddress(it, address)
                    }
                    response?.code()
                } catch (e: Exception) {
                    e.printStackTrace()
                    e.message
                }
                if (result == 200) {
                    status_text.text = getString(R.string.walletconnect_waiting_for_app, currentSession?.dappName)
                } else {
                    alert(getString(R.string.walletconnect_problem_connecting, currentSession?.dappName)) {
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
}
