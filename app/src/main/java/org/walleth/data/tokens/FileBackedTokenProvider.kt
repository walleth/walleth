package org.walleth.data.tokens

import android.content.Context
import com.squareup.moshi.Moshi
import okio.Okio
import org.kethereum.ETHEREUM_NETWORK_MAIN
import org.kethereum.ETHEREUM_NETWORK_RINKEBY
import org.walleth.data.exchangerate.ETH_TOKEN
import org.walleth.data.exchangerate.TokenProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import java.io.File

class TokenDefinitionHolder {
    val tokenDefinitions: Map<String, MutableList<TokenDescriptor>> =
            mutableMapOf(
                    ETHEREUM_NETWORK_MAIN to mutableListOf(
                            ETH_TOKEN
                    ),
                    ETHEREUM_NETWORK_RINKEBY to mutableListOf(
                            ETH_TOKEN,
                            TokenDescriptor("TEA", 5, "0xd9100248636b49ad92d0a9aaafd38bb2abf6355f"),
                            TokenDescriptor("WALL", 15, "0x0a057a87ce9c56d7e336b417c79cf30e8d27860b")
                    ))

}

class FileBackedTokenProvider(context: Context, val networkDefinitionProvider: NetworkDefinitionProvider) : TokenProvider {

    val backingFile = File(context.filesDir, "token_definitions.json")
    val adapter = Moshi.Builder().build().adapter(TokenDefinitionHolder::class.java)!!

    val holder = if (backingFile.exists()) {
        adapter.fromJson(Okio.buffer(Okio.source(backingFile)))!!
    } else {
        TokenDefinitionHolder()
    }

    fun save() {
        Okio.buffer(Okio.sink(backingFile)).use {
            it.writeUtf8(adapter.toJson(holder))
        }
    }

    override var currentToken = ETH_TOKEN

    override fun getAllTokens() = holder.tokenDefinitions[networkDefinitionProvider.currentDefinition.getNetworkName()]!!.toList()

    override fun addToken(tokenDescriptor: TokenDescriptor) {
        holder.tokenDefinitions[networkDefinitionProvider.currentDefinition.getNetworkName()]!!.add(tokenDescriptor)
        save()
    }

}


