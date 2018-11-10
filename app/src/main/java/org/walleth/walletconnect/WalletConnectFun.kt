package org.walleth.walletconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.walleth.activities.walletconnect.WalletConnectErrorActivity
import org.walleth.activities.walletconnect.WalletConnectSendTransactionActivity
import org.walleth.activities.walletconnect.WalletConnectSignTextActivity
import org.walleth.khex.clean0xPrefix
import java.math.BigInteger

fun String.isWalletConnectJSON() = try {
    JSONObject(this).let {
        it.has("domain") && (it.has("sessionId") && it.has("sharedKey") && it.has("dappName"))
    }
} catch (e: Exception) {
    false
}

fun Context.createIntentForTransaction(statefulRPCCall: WalletConnectDriver.StatefulJSONRPCCall): Intent {
    val rpcCall = statefulRPCCall.call
    val params = JSONArray(rpcCall.paramsJSON)

    if (rpcCall.method == "eth_sendTransaction") {
        val transactionParameters = params.getJSONObject(0)

        // TODO - check from

        val url = ERC681(scheme = "ethereum",
                address = transactionParameters.getString("to"),
                value = BigInteger(transactionParameters.getString("value").clean0xPrefix(), 16),
                gas = BigInteger(transactionParameters.getString("gasLimit").clean0xPrefix(), 16)
        ).generateURL()

        return Intent(this, WalletConnectSendTransactionActivity::class.java).apply {
            data = Uri.parse(url)
            setWalletConnectExtras(statefulRPCCall)
            if (transactionParameters.has("data")) {
                val string = transactionParameters.getString("data")
                putExtra("data", string)
            }

            putExtra("gasPrice", transactionParameters.getString("gasPrice"))
            putExtra("nonce", transactionParameters.getString("nonce"))
            putExtra("from", transactionParameters.getString("to"))
            putExtra("parityFlow", false)
        }
    }
    if (rpcCall.method == "eth_sign") {

        return Intent(this, WalletConnectSignTextActivity::class.java).apply {
            putExtra(Intent.EXTRA_TEXT, params.getString(1))

            setWalletConnectExtras(statefulRPCCall)
        }

    }
    return Intent(this, WalletConnectErrorActivity::class.java).apply {
        putExtra(Intent.EXTRA_TEXT, "Err unknown method: " + rpcCall.method)
    }

}

private fun Intent.setWalletConnectExtras(statefulRPCCall: WalletConnectDriver.StatefulJSONRPCCall) {
    putExtra(INTENT_KEY_WC_SESSION_ID, statefulRPCCall.session.sessionId)
    putExtra(INTENT_KEY_WC_CALL_ID, statefulRPCCall.call.id)
}


