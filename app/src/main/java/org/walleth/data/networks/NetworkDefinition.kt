package org.walleth.data.networks

import org.walleth.data.blockexplorer.EtherscanBlockExplorer

interface NetworkDefinition {
    fun getNetworkName(): String
    fun getBlockExplorer(): EtherscanBlockExplorer
    val chainId: Long
    val genesis: String
    val bootNodes: List<String>
}