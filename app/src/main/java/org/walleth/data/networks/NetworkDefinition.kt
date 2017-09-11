package org.walleth.data.networks

import org.walleth.data.blockexplorer.EtherscanBlockExplorer

data class ChainDefinition(
        val id: Long,
        private val prefix: String = "ETH") {
    override fun toString() = prefix + ":" + id
}

interface NetworkDefinition {
    fun getNetworkName(): String
    fun getBlockExplorer(): EtherscanBlockExplorer

    val chain: ChainDefinition
    val genesis: String
    val bootNodes: List<String>
}