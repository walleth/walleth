package org.walleth.data.transactions

import org.walleth.data.WallethAddress
import org.walleth.functions.fromHexToByteArray
import org.walleth.functions.toHexString
import java.math.BigInteger

fun Transaction.isTokenTransfer() = input.startsWith(tokenTransferSignature)
fun Transaction.getTokenTransferValue() = BigInteger(input.subList(input.size - 32, input.size).toHexString(""), 16)
fun Transaction.getTokenTransferTo() = WallethAddress(input.subList(input.size - 32 - 20, input.size - 32).toHexString())

fun List<Byte>.startsWith(prefix: List<Byte>): Boolean {
    if (prefix.size > this.size)
        return false
    (0..(prefix.size) - 1).forEach {
        if (this[it] != prefix[it])
            return false
    }
    return true
}

val tokenTransferSignature = listOf(0xa9.toByte(), 0x05.toByte(), 0x9c.toByte(), 0xbb.toByte())

fun createTokenTransferTransactionInput(address: WallethAddress, currentAmount: BigInteger?): List<Byte>
        = fromHexToByteArray(tokenTransferSignature.toHexString() + "000000000000000000000000" + address.hex.replace("0x", "")
        + String.format("%064x", currentAmount)).toList()
