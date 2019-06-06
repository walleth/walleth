package org.walleth.util;

import org.json.JSONObject

private fun String.checkJSON(foo: (JSONObject) -> Boolean) = try {
    foo(JSONObject(this))
} catch (e: Exception) {
    false
}

fun String.isUnsignedTransactionJSON() = checkJSON {
    it.has("to") && it.has("from") && it.has("chainId") && it.has("nonce") && it.has("value")
            && it.has("gasLimit") && it.has("gasPrice") && it.has("data") && it.has("nonce")
}

fun String.isSignedTransactionJSON() = checkJSON {
    it.has("signedTransactionRLP") && it.has("chainId")
}

fun String.isParityUnsignedTransactionJSON() = checkJSON {
    it.has("action") && it.getString("action") == "signTransaction" && it.has("data")
}

fun String.isJSONKey() = try {
    JSONObject(this).let {
        it.has("address") && (it.has("crypto") || it.has("Crypto"))
    }
} catch (e: Exception) {
    false
}
