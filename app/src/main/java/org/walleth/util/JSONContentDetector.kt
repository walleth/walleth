package org.walleth.util;

import org.json.JSONObject


fun String.isUnsignedTransactionJSON() = try {
    JSONObject(this).let {
        it.has("to") && it.has("from") && it.has("chainId") && it.has("nonce") && it.has("value")
                && it.has("gasLimit") && it.has("gasPrice") && it.has("data") && it.has("nonce")
    }
} catch (e: Exception) {
    false
}

fun String.isSignedTransactionJSON() = try {
    JSONObject(this).let {
        it.has("signedTransactionRLP") && it.has("chainId")
    }
} catch (e: Exception) {
    false
}
