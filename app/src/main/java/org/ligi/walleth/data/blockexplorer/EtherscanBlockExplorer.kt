package org.ligi.walleth.data.blockexplorer

import org.ligi.walleth.data.WallethAddress

open class EtherscanBlockExplorer : BlockExplorer {

    open val base = "https://etherscan.io/"

    override fun getURLforAddress(address: WallethAddress) = "$base/address/${address.hex}"
    override fun getURLforTransaction(transactionHash: String) = "$base/tx/$transactionHash"
    override fun getURLforBlock(blockNum: Long) ="$base/block/$blockNum"

}