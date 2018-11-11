package org.walleth.activities

import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance
import org.walleth.walletconnect.INTENT_KEY_WCSESSIONID
import org.walleth.walletconnect.INTENT_KEY_WCTXID
import org.walleth.walletconnect.WalletConnectDriver

private const val ACTIVITY_RESULT_TXHASH = 10589


class WalletConnectSendTransactionActivity : WallethActivity() {

    private val walletConnectDriver: WalletConnectDriver by instance()

    private val transactionId by lazy { intent.getStringExtra(INTENT_KEY_WCTXID) }
    private val sessionId by lazy { intent.getStringExtra(INTENT_KEY_WCSESSIONID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val newIntent = Intent(this, CreateTransactionActivity::class.java)
        newIntent.data = intent.data
        newIntent.putExtras(intent)

        startActivityForResult(newIntent, ACTIVITY_RESULT_TXHASH)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        var transactionHash = ""

        data?.let {
            when (requestCode) {
                ACTIVITY_RESULT_TXHASH -> {
                    if (data.hasExtra("TXHASH")) {
                        transactionHash = data.getStringExtra("TXHASH")
                    }
                }
            }

        }
        GlobalScope.launch {
            walletConnectDriver.setTransactionHash(transactionId, sessionId, transactionHash , transactionHash.isNotEmpty())
        }
        finish()
    }
}
