package org.walleth.functions

import org.walleth.data.tokens.Token
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

fun Token.decimalsInZeroes() = "0".repeat (decimals)

fun BigInteger.toValueString(token: Token): String {
    val inEther = BigDecimal(this).divide(BigDecimal("1" + token.decimalsInZeroes()))
    return inEther.round(MathContext(6, RoundingMode.FLOOR)).stripTrailingZeros().toPlainString()
}
