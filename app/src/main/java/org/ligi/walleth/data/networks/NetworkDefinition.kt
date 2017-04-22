package org.ligi.walleth.data.networks

import org.ligi.walleth.data.blockexplorer.BlockExplorer

interface NetworkDefinition {
    fun getBlockExplorer(): BlockExplorer
    val genesis: String
    val bootNodes: List<String>
}