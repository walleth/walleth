package org.walleth.util

import org.kethereum.model.ChainId
import org.walleth.data.networks.NetworkDefinition
import org.walleth.data.networks.findNetworkDefinition


fun ChainId.getRPCEndpoint() =
        findNetworkDefinition()?.getRPCEndpoint()

fun NetworkDefinition.getRPCEndpoint() =
        rpcEndpoints.random().replace("\${INFURA_API_KEY}", "b032785efb6947ceb18b9e0177053a17")