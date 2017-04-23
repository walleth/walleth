package org.ligi.walleth.iac

import org.ethereum.geth.Address
import org.ligi.walleth.data.WallethAddress

fun Address.toERC67String() = "ethereum:$hex"
fun WallethAddress.toERC67String() = "ethereum:$hex"

class ERC67(url: String) {
    val split = url.split(":")
    val address by lazy { WallethAddress(getHex()) }
    fun isValid() = split.size == 2 && split.first() == "ethereum"
    fun getHex() = split[1]
}