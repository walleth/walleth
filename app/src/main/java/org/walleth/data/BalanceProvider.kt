package org.walleth.data

import java.math.BigInteger

open class BalanceProvider : SimpleObserveable() {

    val balanceMap = mutableMapOf<WallethAddress, BalanceAtBlock>()

    fun getBalanceForAddress(address: WallethAddress): BalanceAtBlock? = balanceMap[address]

    fun setBalance(address: WallethAddress, block: Long, balance: BigInteger) {
        if (balanceMap[address] == null || balanceMap[address]!!.block < block) {
            balanceMap[address] = BalanceAtBlock(block, balance)
            promoteChange()
        }
    }
}