package org.walleth.data.blockexplorer

import android.content.Context
import org.ligi.kaxtui.alert
import org.walleth.data.networks.ChainInfoProvider
import org.walleth.kethereum.blockscout.BlockScoutBlockExplorer
import org.walleth.kethereum.blockscout.getBlockScoutBlockExplorer

class BlockExplorerProvider(var network: ChainInfoProvider) {

    fun get() = getBlockScoutBlockExplorer(network.getCurrentChainId())

    fun getOrAlert(context: Context): BlockScoutBlockExplorer? {
        val result = get()
        if (result == null) {
            context.alert("No blockExplorer found for the current Network")
        }
        return result
    }
}

