package org.walleth.data

import org.kethereum.model.Address
import org.walleth.data.tokens.TokenDescriptor
import java.math.BigInteger

open class BalanceProvider : SimpleObserveable() {

    protected val balanceMap = mutableMapOf<String, BalanceAtBlock>()

    fun getBalanceForAddress(address: Address, tokenDescriptor: TokenDescriptor): BalanceAtBlock? = balanceMap[address.hex + tokenDescriptor.address]

    fun setBalance(address: Address, block: Long, balance: BigInteger, tokenDescriptor: TokenDescriptor) {
        val key = address.hex + tokenDescriptor.address
        if (balanceMap[key] == null || balanceMap[key]!!.block < block) {
            balanceMap[key] = BalanceAtBlock(block, balance, tokenDescriptor)
            promoteChange()
        }
    }
}