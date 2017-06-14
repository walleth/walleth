package org.walleth.functions

import org.walleth.data.tokens.TokenDescriptor
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

fun TokenDescriptor.decimalsInZeroes() = (0..(decimals-1)).map { "0" }.joinToString("")

fun BigInteger.toValueString(tokenDescriptor: TokenDescriptor): String {
    val inEther = BigDecimal(this).divide(BigDecimal("1" + tokenDescriptor.decimalsInZeroes()))
    return inEther.round(MathContext(6, RoundingMode.FLOOR)).stripTrailingZeros().toPlainString()
}
