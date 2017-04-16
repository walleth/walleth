package org.ligi.walleth.data

import org.ethereum.geth.Address
import org.ethereum.geth.Geth

/**
 * To decouple and distinguish from the native Address from geth
 */

data class WallethAddress(val hex: String) {
    fun toGethAddr() = Geth.newAddressFromHex(hex)
}

fun Address.toWallethAddress() = WallethAddress(hex)