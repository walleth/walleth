package org.walleth.data.networks

import org.walleth.data.blockexplorer.EtherscanBlockExplorer

abstract class BaseNetworkDefinition : NetworkDefinition {

    abstract val etherscan_prefix: String

    override fun getBlockExplorer() = EtherscanBlockExplorer(etherscan_prefix)

}