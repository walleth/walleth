package org.walleth.data.networks

import org.walleth.data.blockexplorer.EtherscanBlockExplorer

abstract class BaseNetworkDefinition : NetworkDefinition {

    abstract val etherscanPrefix: String

    override fun getBlockExplorer() = EtherscanBlockExplorer(etherscanPrefix)

}