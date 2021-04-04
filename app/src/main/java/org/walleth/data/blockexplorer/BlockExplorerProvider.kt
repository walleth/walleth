package org.walleth.data.blockexplorer

import android.content.Context
import org.kethereum.model.BlockExplorer
import org.ligi.kaxtui.alert
import org.walleth.chains.ChainInfoProvider
import org.walleth.kethereum.blockscout.getBlockScoutBlockExplorer
import org.walleth.kethereum.etherscan.getEtherScanBlockExplorer

class BlockExplorerProvider(var network: ChainInfoProvider) {

    suspend fun get() = getBlockScoutBlockExplorer(network.getCurrentChainId()) ?: getEtherScanBlockExplorer(network.getCurrentChainId())

    suspend fun getOrAlert(context: Context): BlockExplorer? {
        val result = get()
        if (result == null) {
            context.alert("No blockExplorer found for the current Network")
        }
        return result
    }
}

