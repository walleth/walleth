package org.ligi.walleth.data

import java.math.BigInteger

class BalanceProvider : SimpleObserveable() {

    val balanceMap = mutableMapOf<WallethAddress, BalanceAtBlock>()

    fun getBalanceForAddress(address: WallethAddress): BalanceAtBlock? = balanceMap[address]

    fun setBalance(address: WallethAddress, block: Long, balance: BigInteger) {
        balanceMap[address] = BalanceAtBlock(block, balance)

        promoteChange()
    }
}