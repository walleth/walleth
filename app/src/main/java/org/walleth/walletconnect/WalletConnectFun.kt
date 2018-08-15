package org.walleth.walletconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.walleth.activities.WalletConnectSendTransactionActivity
import org.walleth.khex.clean0xPrefix
import org.walleth.walletconnect.model.StatefulWalletConnectTransaction
import java.math.BigInteger

fun String.isWalletConnectJSON() = try {
    JSONObject(this).let {
        it.has("domain") && (it.has("sessionId") && it.has("sharedKey") && it.has("dappName"))
    }
} catch (e: Exception) {
    false
}

fun Context.createIntentForTransaction(statefulTransaction: StatefulWalletConnectTransaction): Intent {
    val tx = statefulTransaction.tx
    val url = ERC681(scheme = "ethereum",
            address = tx.to,
            value = BigInteger(tx.value.clean0xPrefix(), 16),
            gas = BigInteger(tx.gasLimit.clean0xPrefix(), 16)
    ).generateURL()

    return Intent(this, WalletConnectSendTransactionActivity::class.java).apply {
        data = Uri.parse(url)
        putExtra(INTENT_KEY_WCTXID, statefulTransaction.id)
        putExtra(INTENT_KEY_WCSESSIONID, statefulTransaction.session.sessionId)
        putExtra("nonce", tx.nonce)
        putExtra("data", tx.data)
        putExtra("gasPrice", tx.gasPrice)
        putExtra("from", tx.from)
        putExtra("parityFlow", false)
    }
}
