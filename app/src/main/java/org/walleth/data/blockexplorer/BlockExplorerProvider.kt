package org.walleth.data.blockexplorer

import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.kethereum.etherscan.getEtherScanBlockExplorer

class BlockExplorerProvider(var network: NetworkDefinitionProvider) {

    fun get() = getEtherScanBlockExplorer(network.getCurrent().chain)

}