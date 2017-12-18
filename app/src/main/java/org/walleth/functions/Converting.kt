package org.walleth.functions

import org.ethereum.geth.BigInt
import org.walleth.data.tokens.Token
import org.walleth.khex.prepend0xPrefix
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*

fun BigInteger.toGethInteger() = BigInt(toLong())
fun BigInteger.toHexString() = this.toString(16).prepend0xPrefix()

fun String.extractValueForToken(token: Token) = BigDecimal(this).multiply(token.decimalsAsMultiplicator()).toBigInteger()


// ENGLISH is used until Andoid O becomes the minSDK or the support-lib fixes this problem:
// https://stackoverflow.com/questions/3821539/decimal-separator-comma-with-numberdecimal-inputtype-in-edittext
val inputDecimalFormat = (NumberFormat.getInstance(Locale.ENGLISH) as DecimalFormat).apply {
    isParseBigDecimal = true
}

val decimalSymbols = DecimalFormatSymbols(Locale.ENGLISH).apply { decimalSeparator = '.' }

val sixDigitDecimalFormat = getDecimalFormat(6)
val twoDigitDecimalFormat = getDecimalFormat(2)

fun String.replaceNullDecimals(decimals : Int) = replace("."+"0".repeat(decimals),"")

private fun getDecimalFormat(decimals: Int) = DecimalFormat("#0." + "0".repeat(decimals), decimalSymbols).apply {
    isGroupingUsed = false
}

fun String.asBigDecimal() = inputDecimalFormat.parseObject(this) as BigDecimal