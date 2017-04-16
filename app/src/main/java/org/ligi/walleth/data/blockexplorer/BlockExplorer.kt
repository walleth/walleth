package org.ligi.walleth.data.blockexplorer

import org.ligi.walleth.data.WallethAddress

interface BlockExplorer {
    fun getURLforAddress(address: WallethAddress): String
    fun getURLforTransaction(transactionHash: String): String
    fun getURLforBlock(blockNum: Long): String
}