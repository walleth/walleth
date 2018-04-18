package org.walleth.contracts

import android.content.Context
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kethereum.methodsignatures.FileBackedMethodSignatureStore
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
    private val baseURL = "https://raw.githubusercontent.com/ethereum-lists/4bytes/master/signatures/"

    override fun getSignaturesFor(hex: String): List<ContractFunction> {
        return try {
            signatureStore.get(hex).map {
                ContractFunction(
                        textSignature = it.signature,
                        hexSignature = hex
                )
            }
        } catch (exception: IOException) {
            fetchAndStore(hex)
        }
    }

    private fun fetchAndStore(hex: String): ArrayList<ContractFunction> {
        val signatures = ArrayList<ContractFunction>()
        val cleanHex = hex.replace("0x", "")
        val url = Request.Builder().url("$baseURL$cleanHex").build()
        val newCall: Call = okHttpClient.newCall(url)

        try {
            val executedCall = newCall.execute()
            if (executedCall.code() == 200) {
                val resultString = executedCall.body().use { it?.string() }
                resultString?.split(";")?.forEach {
                    signatures.add(ContractFunction(hex, it))
                    signatureStore.upsert(hex, it)
                }
            }

        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }

        return signatures
    }

}