package org.walleth.data.tokens

import org.kethereum.model.Address
import org.walleth.data.networks.NetworkDefinitionProvider

class CurrentTokenProvider(networkDefinitionProvider: NetworkDefinitionProvider) {
    var currentToken: Token = Token(name = "ETH", address = Address("0x0"), decimals = 18, chain = networkDefinitionProvider.value!!.chain)
}