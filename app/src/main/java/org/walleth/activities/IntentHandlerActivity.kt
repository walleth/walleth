package org.walleth.activities

import android.content.Context
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import org.kethereum.erc681.toERC681
import org.kethereum.model.EthereumURI
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.tokens.isTokenTransfer
import java.math.BigInteger.ZERO

fun Context.getEthereumViewIntent(ethereumString: String) = Intent(this, IntentHandlerActivity::class.java).apply {
    data = Uri.parse(ethereumString)
}

class IntentHandlerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val erc681 = EthereumURI(intent.data.toString()).toERC681()
        if (erc681.valid) {
            if (erc681.address == null || erc681.isTokenTransfer() || erc681.value != null && erc681.value != ZERO) {
                startActivity(Intent(this, CreateTransactionActivity::class.java).apply {
                    setData(intent.data)
                })
                finish()
            } else {
                AlertDialog.Builder(this)
                        .setTitle(R.string.select_action_messagebox_title)
                        .setItems(R.array.scan_hex_choices, { _, which ->
                            when (which) {
                                0 -> {
                                    startCreateAccountActivity(erc681.address!!)
                                    finish()
                                }
                                1 -> {
                                    startActivity(Intent(this, CreateTransactionActivity::class.java).apply {
                                        setData(intent.data)
                                    })
                                    finish()
                                }
                                2 -> {
                                    alert("TODO", "add token definition", OnClickListener { _, _ -> finish() })
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            finish()
                        }
                        .show()

            }
        } else {
            alert(getString(R.string.create_tx_error_invalid_erc67_msg, intent.data.toString()), getString(R.string.create_tx_error_invalid_erc67_title), OnClickListener { _, _ -> finish() })
        }
    }
}
