package org.walleth.activities

import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import org.kethereum.erc681.toERC681
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.tokens.isTokenTransfer
import java.math.BigInteger.ZERO

class IntentHandlerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val erc681 = intent.data.toString().toERC681()
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
    }
}
