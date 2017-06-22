package org.walleth.data.blockexplorer

import org.kethereum.model.Address

open class EtherscanBlockExplorer : BlockExplorer {

    open val base = "https://etherscan.io/"

    override fun getURLforAddress(address: Address) = "$base/address/${address.hex}"
    override fun getURLforTransaction(transactionHash: String) = "$base/tx/$transactionHash"
    override fun getURLforBlock(blockNum: Long) ="$base/block/$blockNum"

}