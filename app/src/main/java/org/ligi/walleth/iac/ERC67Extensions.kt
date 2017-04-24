package org.ligi.walleth.iac

import org.ethereum.geth.Address
import org.ligi.walleth.data.ETH_IN_WEI
import org.ligi.walleth.data.WallethAddress
import java.math.BigDecimal
import java.math.BigInteger

// https://github.com/ethereum/EIPs/issues/67

fun Address.toERC67String() = "ethereum:$hex"
fun WallethAddress.toERC67String() = "ethereum:$hex"
fun WallethAddress.toERC67String(valueInWei: BigInteger) = "ethereum:$hex?value=$valueInWei"
fun WallethAddress.toERC67String(valueInEther: BigDecimal) = toERC67String((valueInEther * BigDecimal(ETH_IN_WEI)).toBigInteger())

class ERC67(val url: String) {
    val split = url.split(":")
    val address by lazy { WallethAddress(getHex()) }
    fun isValid() = split.size == 2 && split.first() == "ethereum"
    fun getHex() = split[1]
}