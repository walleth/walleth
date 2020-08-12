package org.walleth.data.tokens

import org.kethereum.erc681.ERC681
import org.kethereum.model.Address
import java.math.BigInteger

data class TokenTransfer(
        val to: Address,
        val token: Token,
        val value: BigInteger)

fun TokenTransfer.toERC681() = ERC681(address = token.address.hex, function = "transfer",
        functionParams = listOf("address" to to.hex, "uint256" to value.toString()))

fun ERC681.getToAddress(): Address? {
    val address = if (this.function == "transfer") {
        this.functionParams.first { it.first == "address" }.second
    } else {
        this.address
    }
    return address?.let { Address(address) }
}

fun ERC681.isTokenTransfer() = this.function == "transfer"
fun ERC681.getValueForTokenTransfer() = functionParams.firstOrNull { it.first == "uint256" }?.second?.let {
    try {
        BigInteger(it)
    } catch (ignore: NumberFormatException) {
        null
    }
}