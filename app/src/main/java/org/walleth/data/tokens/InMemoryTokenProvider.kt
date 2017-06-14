package org.walleth.data.exchangerate

import org.walleth.data.tokens.TokenDescriptor

class InMemoryTokenProvider : TokenProvider {

    val tokens = listOf(
            ETH_TOKEN,
            TokenDescriptor("TEA", 5, "0xd9100248636b49ad92d0a9aaafd38bb2abf6355f"),
            TokenDescriptor("WALL", 15, "0x0a057a87ce9c56d7e336b417c79cf30e8d27860b")
    )

    override var currentToken = tokens.first()

    override fun getAllTokens() = tokens

}


