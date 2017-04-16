package org.ligi.walleth.data

import org.ligi.walleth.App
import java.math.BigInteger

data class BalanceAtBlock(val block: Long, val balance: BigInteger)

object BalanceProvider {

    val balanceMap = mutableMapOf<WallethAddress, BalanceAtBlock>()

    fun getBalanceForAddress(address: WallethAddress): BalanceAtBlock? = balanceMap[address]

    fun setBalance(address: WallethAddress, block: Long, balance: BigInteger) {
        balanceMap[address] = BalanceAtBlock(block, balance)
        App.bus.post(BalanceUpdate)
    }
}