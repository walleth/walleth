package org.walleth.functions

import android.content.Context
import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.walleth.R
import org.walleth.contracts.FourByteDirectory
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.resolveName
import org.walleth.data.transactions.FunctionCall
import org.walleth.kethereum.model.ContractFunction
import java.math.BigInteger

object SignatureHash {
    val tokenTransfer = "a9059cbb"
}

fun String.toCleanHex() =
        if (this.startsWith("0x")) {
            this.substring(2)
        } else {
            this
        }

/**
 * for clean hex input strings only
 */
fun String.toFunctionCall(fourByteDirectory: FourByteDirectory): FunctionCall? {
    if (this.length >= 8) {
        val signatures = fourByteDirectory.getSignaturesFor(this.substring(0, 8))
        if (signatures.size == 1 && signatures[0].hexSignature == SignatureHash.tokenTransfer) {
            return FunctionCall(signatures[0].toParametersFor(this)[0] as Address, null)
        }
    }
    return null
}

fun ContractFunction.toParametersFor(cleanInput: String): List<Any?> {
    if (arguments == null) {
        return listOf()
    }

    val parameters = ArrayList<Any?>(arguments.size)
    var point = 8
    arguments.forEachIndexed { index, type ->
        val value = when (type) {
            "address" -> Address(cleanInput.substring(point + 24, point + 64)).also { point += 64 }
            "uint256" -> BigInteger(cleanInput.substring(point, point + 64), 16).also { point += 64 }
            else -> return parameters // no further parsing
        }
        parameters.add(value)
    }
    return parameters
}

fun List<ContractFunction>.toHumanReadableTextFor(cleanInput: String, transaction: Transaction, appDatabase: AppDatabase, context: Context): String {
    return when {
        isEmpty() -> "-"
        (size == 1 && get(0).hexSignature == SignatureHash.tokenTransfer) -> {
            val token = appDatabase.tokens.forAddress(transaction.to!!)
            val parameters = get(0).toParametersFor(cleanInput)
            val receiverName = appDatabase.addressBook.resolveName(parameters[0] as Address)
            if (token != null) {
                context.getString(R.string.token_transfer, (parameters[1] as BigInteger).toValueString(token), token.symbol, receiverName)
            } else {
                context.getString(R.string.unknown_token_transfer, receiverName)
            }
        }
        else -> joinToString(
                separator = " ${context.getString(R.string.or)}\n",
                transform = { sig ->
                    sig.textSignature ?: sig.hexSignature
                })
    }
}
