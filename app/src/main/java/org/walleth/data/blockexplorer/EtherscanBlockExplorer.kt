package org.walleth.data.blockexplorer

import org.walleth.data.WallethAddress

open class EtherscanBlockExplorer : BlockExplorer {

    open val base = "https://etherscan.io/"

    override fun getURLforAddress(address: WallethAddress) = "$base/address/${address.hex}"
    override fun getURLforTransaction(transactionHash: String) = "$base/tx/$transactionHash"
    override fun getURLforBlock(blockNum: Long) ="$base/block/$blockNum"

}