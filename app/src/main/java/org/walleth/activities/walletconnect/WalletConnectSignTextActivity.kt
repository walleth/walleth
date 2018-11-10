package org.walleth.activities.walletconnect

import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.walleth.activities.SignTextActivity
import org.walleth.activities.WallethActivity
import org.walleth.walletconnect.INTENT_KEY_WC_CALL_ID
import org.walleth.walletconnect.INTENT_KEY_WC_SESSION_ID
import org.walleth.walletconnect.WalletConnectDriver

private const val ACTIVITY_REQUEST_ID_WC_ACTION = 10589


class WalletConnectSignTextActivity : WallethActivity() {

    private val walletConnectDriver: WalletConnectDriver by inject()

    private val callId by lazy { intent.getStringExtra(INTENT_KEY_WC_CALL_ID) }
    private val sessionId by lazy { intent.getStringExtra(INTENT_KEY_WC_SESSION_ID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val newIntent = Intent(this, SignTextActivity::class.java)
        newIntent.data = intent.data
        newIntent.putExtras(intent)

        startActivityForResult(newIntent, ACTIVITY_REQUEST_ID_WC_ACTION)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        var result = ""

        data?.let {
            when (requestCode) {
                ACTIVITY_REQUEST_ID_WC_ACTION -> {
                    if (data.hasExtra("SIGNATURE")) {
                        result = data.getStringExtra("SIGNATURE")
                    }
                }
            }

        }
        GlobalScope.launch {
            walletConnectDriver.setResult(callId, sessionId, result, result.isNotEmpty())
            GlobalScope.launch(Dispatchers.Main) {
                finish()
            }
        }
    }
}
