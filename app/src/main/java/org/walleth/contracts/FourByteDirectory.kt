package org.walleth.contracts

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.walleth.kethereum.model.ContractFunction
import java.io.IOException

interface FourByteDirectory {
    fun getSignatureFor(hex: String): ContractFunction?
}

class FourByteDirectoryImpl(private val okHttpClient: OkHttpClient) : FourByteDirectory {
    private val baseURL = "https://www.4byte.directory/api/v1"

    override fun getSignatureFor(hex: String): ContractFunction? {
        val urlString = "$baseURL/signatures/?hex_signature=$hex"
        val url = Request.Builder().url(urlString).build()
        val newCall: Call = okHttpClient.newCall(url)

        try {
            val resultString = newCall.execute().body().use { it?.string() }
            return resultString?.fourByteResultToSingleContractFunction()

        } catch (ioe: IOException) {
            ioe.printStackTrace()
        } catch (jsonException: JSONException) {
            jsonException.printStackTrace()
        }
        return null
    }
}

fun String.fourByteResultToSingleContractFunction(): ContractFunction? {
    return JSONObject(this).let {
        val count = it.getInt("count")
        if (count >= 1) {
            it.getJSONArray("results").get(0) as JSONObject
        } else {
            null
        }
    }?.let {
        ContractFunction(
                textSignature = it.getString("text_signature"),
                hexSignature = it.getString("hex_signature")
        )
    }
}