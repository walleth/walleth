package org.ligi.walleth.data

import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.lazy
import org.greenrobot.eventbus.EventBus
import org.ligi.walleth.App
import java.math.BigInteger

data class BalanceAtBlock(val block: Long, val balance: BigInteger)

object BalanceProvider {

    val balanceMap = mutableMapOf<WallethAddress, BalanceAtBlock>()
    val bus: EventBus by App.kodein.lazy.instance()

    fun getBalanceForAddress(address: WallethAddress): BalanceAtBlock? = balanceMap[address]

    fun setBalance(address: WallethAddress, block: Long, balance: BigInteger) {
        balanceMap[address] = BalanceAtBlock(block, balance)

        bus.post(BalanceUpdate)
    }
}