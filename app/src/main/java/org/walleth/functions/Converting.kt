package org.walleth.functions

import org.ethereum.geth.BigInt
import org.walleth.data.tokens.Token
import org.walleth.khex.prepend0xPrefix
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.NumberFormat

fun BigInteger.toGethInteger() = BigInt(toLong())
fun BigInteger.toHexString() = this.toString(16).prepend0xPrefix()

fun String.extractValueForToken(token: Token) = BigDecimal(this).multiply(token.decimalsAsMultiplicator()).toBigInteger()

fun String.asBigDecimal() = (NumberFormat.getInstance() as DecimalFormat).apply {
    isParseBigDecimal = true
}.parseObject(this) as BigDecimal