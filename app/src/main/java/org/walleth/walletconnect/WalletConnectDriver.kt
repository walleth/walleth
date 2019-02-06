package org.walleth.walletconnect

import android.content.Context
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import org.kethereum.model.Address
import org.ligi.tracedroid.logging.Log
import org.walletconnect.PlainTextData
import org.walletconnect.decrypt
import org.walletconnect.encrypt
import org.walletconnect.model.CryptoContainer
import org.walletconnect.model.EncryptedData
import org.walleth.data.JSON_MEDIA_TYPE
import org.walleth.khex.hexToByteArray
import org.walleth.walletconnect.model.Session
import java.io.File

open class WalletConnectDriver(
        private val context: Context,
        private val pushServerURL: String,
        private val okHttpClient: OkHttpClient,
        private val moshi: Moshi = Moshi.Builder().build()) {

    var txAction: ((tx: StatefulJSONRPCCall) -> Unit)? = null
    var fcmToken = ""

    private val sessionStore by lazy { SessionStore(File(context.cacheDir, "walletconnect_sessionstore.json")) }
    private val encryptedDataAdapter by lazy {
        moshi.adapter(EncryptedData::class.java)
    }

    private var aad = 1

    fun hasFCMToken() = fcmToken.isNotBlank()

    fun sendAddress(session: Session, address: Address): Response? {
        sessionStore.put(session)

        val dataToEncrypt = """{"data":{"accounts":["$address"],"approved":true}}""".toByteArray()

        val encryptedJSON = PlainTextData(dataToEncrypt).encrypt(session.sharedKey.hexToByteArray())

        val payload = """
                    { "encryptionPayload" : ${encryptedDataAdapter.toJson(encryptedJSON)} ,
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

            val encryptedData = PlainTextData(dataToEncrypt).encrypt(session.sharedKey.hexToByteArray())

            val payload = """{ "encryptionPayload" : ${encryptedDataAdapter.toJson(encryptedData)} ,"aad": $aad}"""
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

    fun getCalls(sessionId: String) = sessionStore.get(sessionId)?.let { session ->
        val url = "${session.domain}/session/${session.sessionId}/calls"

        var callId: String? = null

        val sessionData = okHttpClient.newCall(Request.Builder()
                .url(url).build())
                .execute().use { it.body().use { it?.string() } }
                ?.let {
                    JSONObject(it).getJSONObject("data").let { sessionData ->
                        callId = sessionData.keys().next()
                        sessionData.getString(callId)
                    }
                }

        if (sessionData == null) {
            Log.w("Could not get session data from $url")
            return null
        }

        val adapter = moshi.adapter(CryptoContainer::class.java)

        val cryptoContainer = adapter.fromJson(sessionData)

        val decryptedData = cryptoContainer?.encryptionPayload?.decrypt(session.sharedKey.hexToByteArray())


        if (decryptedData == null) {
            Log.w("Could not decrypt data")
            return null
        }

        val rpcCall = JSONObject(String(decryptedData)).getJSONObject("data")

        val method = rpcCall.getString("method")
        val params = rpcCall.getString("params")
        StatefulJSONRPCCall(session, JSONRPCCall(method = method, paramsJSON = params, id = callId!!))
    }

}
