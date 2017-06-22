package org.walleth.data.blockexplorer

import org.kethereum.model.Address


interface BlockExplorer {
    fun getURLforAddress(address: Address): String
    fun getURLforTransaction(transactionHash: String): String
    fun getURLforBlock(blockNum: Long): String
}