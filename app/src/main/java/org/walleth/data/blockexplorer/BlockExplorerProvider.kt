package org.walleth.data.blockexplorer

import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.kethereum.blockscout.getBlockScoutBlockExplorer

class BlockExplorerProvider(var network: NetworkDefinitionProvider) {

    fun get() = getBlockScoutBlockExplorer(network.getCurrent().chain)

}