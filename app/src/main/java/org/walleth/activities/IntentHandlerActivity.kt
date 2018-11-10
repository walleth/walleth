package org.walleth.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import org.kethereum.erc1328.isERC1328
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.isERC681
import org.kethereum.erc681.toERC681
import org.kethereum.erc831.isERC831
import org.kethereum.erc831.toERC831
import org.kethereum.model.Address
import org.kethereum.model.EthereumURI
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.walletconnect.WalletConnectConnectionActivity
import org.walleth.data.config.Settings
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.tokens.isTokenTransfer
import java.math.BigInteger.ZERO

private const val CREATE_TX_REQUEST_CODE = 10123
private const val SIGN_TX_REQUEST_CODE = 10124

fun Context.getEthereumViewIntent(ethereumString: String) = Intent(this, IntentHandlerActivity::class.java).apply {
    data = Uri.parse(ethereumString)
}

class IntentHandlerActivity : WallethActivity() {

    var textToSign: String? = null

    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val keyStore: WallethKeyStore by inject()
    val settings: Settings by inject()
    private var oldFilterAddressesKeyOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parsed831 = EthereumURI(intent.data.toString()).toERC831()

        if (parsed831.isERC831()) {
            if (parsed831.isERC681()) {
                val erC681 = parsed831.toERC681()
                if (erC681.valid) {
                    process681(erC681)
                } else {
                    alert("This URL is illegal ERC-681. Please contact the provider to change this!") {
                        finish()
                    }
                }
            }

            if (parsed831.isERC1328()) {
                val wcIntent = Intent(this, WalletConnectConnectionActivity::class.java)
                wcIntent.data = intent.data
                startActivity(wcIntent)
                finish()
            }

            if (parsed831.prefix == "esm") {
                val split = parsed831.payload?.split("/")
                if (split?.size != 2) {
                    alert("Invalid request. " + intent.data.toString() + " If you think this should be valid - please drop ligi a mail (ligi@ligi.de).")
                    return
                }

                textToSign = split.last()
                if (split.first().isEmpty()) {
                    oldFilterAddressesKeyOnly = settings.filterAddressesKeyOnly
                    settings.filterAddressesKeyOnly = true
                    val intent = Intent(this, AddressBookActivity::class.java)
                    startActivityForResult(intent, TO_ADDRESS_REQUEST_CODE)
                } else {
                    val wantedAddress = Address(split.first())

                    if (keyStore.hasKeyForForAddress(wantedAddress)) {
                        currentAddressProvider.setCurrent(wantedAddress)
                        startEthereumSignedMessage()
                    } else {
                        alert("Don't have the key for the requested address $wantedAddress")
                    }
                }
            }

        } else {
            alert(getString(R.string.create_tx_error_invalid_erc67_msg, intent.data.toString()), getString(R.string.create_tx_error_invalid_erc67_title)) {
                finish()
            }
        }
    }


    private fun process681(erC681: ERC681) {
        if (erC681.address == null || erC681.isTokenTransfer() || erC681.value != null && erC681.value != ZERO) {
            startActivity(Intent(this, CreateTransactionActivity::class.java).apply {
                data = intent.data
            })
            finish()
        } else {
            AlertDialog.Builder(this)
                    .setTitle(R.string.select_action_messagebox_title)
                    .setItems(R.array.scan_hex_choices) { _, which ->
                        when (which) {
                            0 -> {
                                startCreateAccountActivity(erC681.address!!)
                                finish()
                            }
                            1 -> {
                                val intent = Intent(this, CreateTransactionActivity::class.java).apply {
                                    data = intent.data
                                }
                                startActivityForResult(intent, CREATE_TX_REQUEST_CODE)
                            }
                            2 -> alert("TODO", "add token definition") {
                                finish()
                            }

                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        finish()
                    }
                    .setOnCancelListener {
                        finish()
                    }
                    .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            SIGN_TX_REQUEST_CODE, CREATE_TX_REQUEST_CODE -> {
                setResult(resultCode, data)
                finish()
            }

            TO_ADDRESS_REQUEST_CODE -> {

                if (data?.hasExtra("HEX") == true) {
                    currentAddressProvider.setCurrent(Address(data.getStringExtra("HEX")))
                }

                settings.filterAddressesKeyOnly = oldFilterAddressesKeyOnly
                startEthereumSignedMessage()
            }
        }
    }

    private fun startEthereumSignedMessage() {
        val intent = Intent(this, SignTextActivity::class.java)
        intent.putExtra("TEXT", textToSign)
        startActivityForResult(intent, SIGN_TX_REQUEST_CODE)
    }
}
