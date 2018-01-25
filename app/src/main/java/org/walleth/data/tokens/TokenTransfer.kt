package org.walleth.data.tokens

import org.kethereum.erc681.ERC681
import org.kethereum.model.Address
import java.math.BigInteger

data class TokenTransfer(
        val to: Address,
        val token: Token,
        val value: BigInteger)

fun TokenTransfer.toERC681() = ERC681(address = token.address.hex, function = "transfer",
        functionParams = mapOf("address" to to.hex, "uint256" to value.toString()))

fun ERC681.getToAddress(): Address? {
    val address = if (this.function == "transfer") {
        this.functionParams["address"]
    } else {
        this.address
    }
    if (address != null) {
        return Address(address)
    } else {
        return null
    }
}

fun ERC681.isTokenTransfer() = this.function == "transfer"
fun ERC681.getValueForTokenTransfer(): BigInteger {
    val value = this.functionParams["uint256"]
    if (value != null) {
        try {
            return BigInteger(value)
        } catch (ignore: NumberFormatException) {
            return BigInteger.ZERO
        }
    } else {
        return BigInteger.ZERO
    }
}