package org.walleth.data.tokens

import org.walleth.data.networks.NetworkDefinitionProvider

class CurrentTokenProvider(networkDefinitionProvider: NetworkDefinitionProvider) {
    var currentToken: Token = getEthTokenForChain(networkDefinitionProvider.getCurrent())
}