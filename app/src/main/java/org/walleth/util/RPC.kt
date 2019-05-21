package org.walleth.util

import org.walleth.data.chaininfo.ChainInfo


fun ChainInfo.getRPCEndpoint() = if (rpc.isNotEmpty()) {
    rpc.random().replace("\${INFURA_API_KEY}", "b032785efb6947ceb18b9e0177053a17")
} else { null }