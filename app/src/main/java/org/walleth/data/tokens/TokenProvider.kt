package org.walleth.data.exchangerate

import org.walleth.data.tokens.TokenDescriptor

val ETH_TOKEN = TokenDescriptor("ETH", 18, "0x0")

interface TokenProvider {
    fun getAllTokens(): List<TokenDescriptor>
    var currentToken: TokenDescriptor
}


