package org.walleth.data.transactions

import org.kethereum.model.Address
import org.walleth.contracts.FourByteDirectory
import org.walleth.kethereum.model.ContractFunction
import java.math.BigInteger

data class FunctionCall(var relevantAddress1: Address?, var relevantAddress2: Address? = null)

fun String.toFunctionCall(fourByteDirectory: FourByteDirectory): FunctionCall? {
    val signatures = fourByteDirectory.getSignaturesFor(this)
    if (signatures.size == 1 && signatures[0].hexSignature == Signatures.tokenTransfer) {
        return FunctionCall(signatures[0].parametersFrom(this)[0] as Address, null)
    }
    return null
}

fun ContractFunction.parametersFrom(input: String): List<Any?> {
    if (arguments == null) {
        return listOf()
    }

    val parameters = ArrayList<Any?>(arguments.size)
    var point = 8
    arguments.forEachIndexed { index, type ->
        val value = when (type) {
            "address" -> Address(input.substring(point + 24, point + 64)).also { point += 64 }
            "uint256" -> BigInteger(input.substring(point, point + 64), 16).also { point += 64 }
            else -> return parameters // no further parsing
        }
        parameters.add(value)
    }
    return parameters
}

object Signatures {
    val tokenTransfer = "a9059cbb"
}
