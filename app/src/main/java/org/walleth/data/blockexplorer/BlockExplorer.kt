package org.walleth.data.blockexplorer

import org.walleth.data.WallethAddress

interface BlockExplorer {
    fun getURLforAddress(address: WallethAddress): String
    fun getURLforTransaction(transactionHash: String): String
    fun getURLforBlock(blockNum: Long): String
}