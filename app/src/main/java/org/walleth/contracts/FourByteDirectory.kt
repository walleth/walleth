package org.walleth.contracts

import android.content.Context
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.kethereum.methodsignatures.FileBackedMethodSignatureStore
import org.walleth.functions.JSONObjectIterator
import org.walleth.kethereum.model.ContractFunction
import java.io.File
import java.io.IOException

interface FourByteDirectory {
    fun getSignaturesFor(hex: String): List<ContractFunction>
}

class FourByteDirectoryImpl(private val okHttpClient: OkHttpClient, context: Context) : FourByteDirectory {
    private val storeDir = File(context.cacheDir, "funsignatures").apply {
        mkdirs()
    }
    private val signatureStore = FileBackedMethodSignatureStore(storeDir)
    private val baseURL = "https://www.4byte.directory/api/v1"

    override fun getSignaturesFor(hex: String): List<ContractFunction> {
        return try {
            signatureStore.get(hex).map {
                ContractFunction(
                        textSignature = it,
                        hexSignature = hex
                )
            }
        } catch (exception: IOException) {
            fetchAndStore(hex)
        }
    }

    private fun fetchAndStore(hex: String): ArrayList<ContractFunction> {
        val signatures = ArrayList<ContractFunction>()
        val urlString = "$baseURL/signatures/?hex_signature=$hex"
        val url = Request.Builder().url(urlString).build()
        val newCall: Call = okHttpClient.newCall(url)

        try {
            val resultString = newCall.execute().body().use { it?.string() }
            resultString?.fourByteResultToContractFunctions()?.let {
                signatures.addAll(it)
            }

        } catch (ioe: IOException) {
            ioe.printStackTrace()
        } catch (jsonException: JSONException) {
            jsonException.printStackTrace()
        }
        return signatures
    }

    fun String.fourByteResultToContractFunctions(): List<ContractFunction> {
        val signatures = ArrayList<ContractFunction>()
        JSONObject(this).let {
            val count = it.getInt("count")
            if (count >= 1) {
                it.getJSONArray("results").JSONObjectIterator().forEach {
                    val textSignature = it.getString("text_signature")
                    val hexSignature = it.getString("hex_signature")
                    signatureStore.upsert(hexSignature, textSignature)

                    signatures.add(ContractFunction(
                            textSignature = textSignature,
                            hexSignature = hexSignature))
                }
            }
        }
        return signatures
    }

}