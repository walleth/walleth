package org.walleth.walletconnect

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
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
import org.walleth.data.JSON_MEDIA_TYPE
import org.walleth.khex.hexToByteArray
import org.walleth.khex.toNoPrefixHexString
import org.walleth.walletconnect.model.Session
import java.io.File
import java.security.SecureRandom

open class WalletConnectDriver(
        private val context: Context,
        private val pushServerURL: String,
        private val okHttpClient: OkHttpClient) {

    var txAction: ((tx: StatefulJSONRPCCall) -> Unit)? = null
    var fcmToken = ""

    private val sessionStore by lazy { SessionStore(File(context.cacheDir, "walletconnect_sessionstore.json")) }

    private var aad = 1

    fun hasFCMToken() = fcmToken.isNotBlank()

    fun sendAddress(session: Session, address: Address): Response? {
        sessionStore.put(session)

        val dataToEncrypt = """{"data":{"accounts":["$address"],"approved":true}}""".toByteArray()

        val encryptedJSON = encrypt(session.sharedKey.hexToByteArray(), dataToEncrypt)

        val payload = """
                    { "encryptionPayload" : $encryptedJSON ,
                      "push": {
                    "type":"fcm",
                    "token":"$fcmToken",
                    "webhook":"$pushServerURL"
                    }
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

    fun setResult(transactionId: String,
                  sessionId: String,
                  hash: String,
                  success: Boolean = true) {

        sessionStore.get(sessionId)?.let { session ->

            val dataToEncrypt = (
                    """{ "data" : {"approved":$success """ +
                            (if (success) ""","result": "0x$hash"""" else "") +
                            "}}"
                    ).toByteArray()

            val encryptedJSON = encrypt(session.sharedKey.hexToByteArray(), dataToEncrypt)

            val payload = """{ "encryptionPayload" : $encryptedJSON ,"aad": $aad}"""
            val url = "${session.domain}/call-status/$transactionId/new"

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

    class JSONRPCCall(val method: String, val paramsJSON: String, val id: String)
    class StatefulJSONRPCCall(val session: Session, val call: JSONRPCCall)

    fun getCalls(sessionId: String): StatefulJSONRPCCall? {
        sessionStore.get(sessionId)?.let { session ->
            val url = "${session.domain}/session/${session.sessionId}/calls"

            val sessionData = okHttpClient.newCall(Request.Builder()
                    .url(url).build())
                    .execute().use { it.body().use { it?.string() } }
                    ?.let {
                        JSONObject(it).getJSONObject("data").let { sessionData ->
                            val callId = sessionData.keys().next()
                            sessionData.getJSONObject(callId).getJSONObject("encryptionPayload").put("callId", callId)
                        }
                    }

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

            val jsonString = String(outBuf.copyOf(length1 + length2))
            val rpcCall = JSONObject(jsonString).getJSONObject("data")

            val method = rpcCall.getString("method")
            val params = rpcCall.getString("params")
            val id = sessionData.getString("callId")
            return StatefulJSONRPCCall(session, JSONRPCCall(method = method, paramsJSON = params, id = id))
        }

    }

}
