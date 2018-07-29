package org.walleth.walletconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.model.Address
import org.ligi.tracedroid.logging.Log
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.macs.HMac
import org.spongycastle.crypto.modes.CBCBlockCipher
import org.spongycastle.crypto.paddings.PKCS7Padding
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.crypto.params.ParametersWithIV
import org.walleth.activities.WalletConnectSendTransactionActivity
import org.walleth.data.JSON_MEDIA_TYPE
import org.walleth.khex.clean0xPrefix
import org.walleth.khex.hexToByteArray
import org.walleth.khex.toNoPrefixHexString
import java.io.File
import java.math.BigInteger
import java.security.SecureRandom


fun String.isWalletConnectJSON() = try {
    JSONObject(this).let {
        it.has("domain") && (it.has("sessionId") && it.has("sharedKey") && it.has("dappName"))
    }
} catch (e: Exception) {
    false
}

class WalletConnectTransaction(val from: String,
                               val to: String,
                               val nonce: String,
                               val gasPrice: String,
                               val gasLimit: String,
                               val gas: String,
                               val value: String,
                               val data: String)

class StatefulWalletConnectTransaction(val tx: WalletConnectTransaction,
                                       val session: Session,
                                       val id: String)


class WrappedWalletConnectTransaction(val data: WalletConnectTransaction)

data class Session(val sessionId: String,
                   val domain: String,
                   val dappName: String,
                   val sharedKey: String)

data class SessionList(val sessions: List<Session>)

const val INTENT_KEY_WCTXID = "WalletConnectTransactionId"
const val INTENT_KEY_WCSESSIONID = "WalletConnectSessionId"

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

class SessionStore(val direcory: File) {

    private val sessionListAdapter = Moshi.Builder().build().adapter(SessionList::class.java)

    private val sessionMap by lazy {
        mutableMapOf<String, Session>().apply {
            sessionListAdapter.fromJson(direcory.readText())?.sessions?.forEach { session ->
                put(session.sessionId, session)
            }
        }
    }

    fun put(session: Session) {
        sessionMap[session.sessionId] = session

        val sessionJSO = sessionListAdapter.toJson(SessionList(sessionMap.map { it.value }))

        direcory.writeText(sessionJSO)
    }

    fun get(key: String): Session? {
        val session = sessionMap[key]

        if (session == null) {
            Log.w("Cannot find session for id $key in SessionStore")
        }

        return session
    }
}

class WalletConnectDriver(
        private val context: Context,
        private val pushServerURL: String,
        private val okHttpClient: OkHttpClient) {

    val sessionStore by lazy { SessionStore(File(context.cacheDir, "walletconnect_sessionstore.json")) }
    var fcmToken = ""
    var txAction: ((tx: StatefulWalletConnectTransaction) -> Unit)? = null

    private var aad = 1

    fun sendAddress(session: Session, address: Address): Response? {
        sessionStore.put(session)

        val dataToEncrypt = """{"data":["$address"]}""".toByteArray()

        val encryptedJSON = encrypt(session.sharedKey.hexToByteArray(), dataToEncrypt)

        val payload = """
                    { "data" : $encryptedJSON ,
                    "fcmToken":"$fcmToken",
                    "pushEndpoint":"$pushServerURL",
                    "aad": $aad
                    }
                """.trimIndent()
        val url = "${session.domain}/session/${session.sessionId}"

        aad++

        return okHttpClient.newCall(Request.Builder()
                .put(RequestBody.create(JSON_MEDIA_TYPE, payload))
                .url(url).build())
                .execute()

    }


    private fun encrypt(key: ByteArray, dataToEncrypt: ByteArray): String {
        val random = SecureRandom()
        val iv = ByteArray(16)
        random.nextBytes(iv)

        val padding = PKCS7Padding()
        val aes = PaddedBufferedBlockCipher(CBCBlockCipher(AESEngine()), padding)
        val ivAndKey = ParametersWithIV(KeyParameter(key), iv)
        aes.init(true, ivAndKey)

        val minSize = aes.getOutputSize(dataToEncrypt.size)
        val outBuf = ByteArray(minSize)
        val length1 = aes.processBytes(dataToEncrypt, 0, dataToEncrypt.size, outBuf, 0)
        aes.doFinal(outBuf, length1)
        val hmac = HMac(SHA256Digest())
        hmac.init(KeyParameter(key))

        val hmacResult = ByteArray(hmac.macSize)

        val encodedHex = outBuf.toNoPrefixHexString()
        hmac.update(encodedHex.toByteArray(), 0, encodedHex.toByteArray().size)
        val ivHex = iv.toNoPrefixHexString()
        hmac.update(ivHex.toByteArray(), 0, ivHex.toByteArray().size)
        hmac.doFinal(hmacResult, 0)

        return """{ "data" : "${outBuf.toNoPrefixHexString()}" ,
            "hmac": "${hmacResult.toNoPrefixHexString()}",
            "iv": "$ivHex" }""".trimIndent()
    }

    fun setTransactionHash(transactionId: String,
                           sessionId: String,
                           hash: String,
                           success: Boolean = true) {

        sessionStore.get(sessionId)?.let { session ->

            val dataToEncrypt = """{ "data" : {"txHash": "$hash" , "success":$success }}""".toByteArray()

            val encryptedJSON = encrypt(session.sharedKey.hexToByteArray(), dataToEncrypt)

            val payload = """{ "data" : $encryptedJSON ,"aad": $aad}"""
            val url = "${session.domain}/transaction-status/$transactionId/new"

            aad++

            val response = okHttpClient.newCall(Request.Builder()
                    .post(RequestBody.create(JSON_MEDIA_TYPE, payload))
                    .url(url).build())
                    .execute()

            if (response.code() > 201) {
                Log.w("Could not submit transaction hash. response code:" + response.code() + " body:" + response.body()?.string() + " url: $url")
            }
        }
    }

    fun getTransaction(transactionId: String, sessionId: String): StatefulWalletConnectTransaction? {
        sessionStore.get(sessionId)?.let { session ->

            val url = "${session.domain}/session/${session.sessionId}/transaction/$transactionId"

            val sessionData = okHttpClient.newCall(Request.Builder()
                    .url(url).build())
                    .execute().use { it.body().use { it?.string() } }
                    ?.let { JSONObject(it).getJSONObject("data") }

            if (sessionData == null) {
                Log.w("Could not get session data from $url")
                return null
            }

            val data = sessionData.getString("data")!!.hexToByteArray()

            val key = session.sharedKey.hexToByteArray()

            val iv = sessionData.getString("iv")?.hexToByteArray()

            val padding = PKCS7Padding()
            val aes = PaddedBufferedBlockCipher(CBCBlockCipher(AESEngine()), padding)
            val ivAndKey = ParametersWithIV(KeyParameter(key), iv)
            aes.init(false, ivAndKey)

            val minSize = aes.getOutputSize(data.size)
            val outBuf = ByteArray(minSize)
            val length1 = aes.processBytes(data, 0, data.size, outBuf, 0)
            val length2 = aes.doFinal(outBuf, length1)

            val string = String(outBuf.copyOf(length1 + length2))

            val res = Moshi.Builder()
                    .build()
                    .adapter(WrappedWalletConnectTransaction::class.java)
                    .fromJson(string)

            return res?.data?.let { StatefulWalletConnectTransaction(it, session, transactionId) }
        }
        return null
    }
}
