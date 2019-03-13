package org.walleth.functions

import org.walleth.data.tokens.Token
import java.math.BigDecimal
import java.math.BigInteger

fun Token.decimalsInZeroes() = "0".repeat(decimals)
fun Token.decimalsAsMultiplicator() = BigDecimal("1" + decimalsInZeroes())

fun BigInteger.toValueString(token: Token?) = BigDecimal(this).toValueString(token)

fun BigInteger.toFullValueString(token: Token) = BigDecimal(this).toFullValueString(token)
fun BigDecimal.toFullValueString(token: Token) = applyTokenDecimals(token).toString()

fun BigDecimal.applyTokenDecimals(token: Token?): BigDecimal = divide(BigDecimal("1" + (token?.decimalsInZeroes() ?: ""))).stripTrailingZeros()
fun BigDecimal.toValueString(token: Token?) = applyTokenDecimals(token).let { valueInETH ->
    val format = sixDigitDecimalFormat.format(valueInETH)

    val cutFormat = if (format.length > 8 && format.contains('.')) {
        format.substring(0, Math.max(8, format.lastIndexOf('.') + 1))
    } else {
        format
    }
    cutFormat.stripTrailingZeros()
}

fun BigDecimal.toFiatValueString() = twoDigitDecimalFormat.format(this)
        .stripTrailingZeros()
        .adjustToMonetary2DecimalsWhenNeeded()

fun String.addPrefixOnCondition(prefix: String, condition: Boolean) = if (condition) prefix + this else this
