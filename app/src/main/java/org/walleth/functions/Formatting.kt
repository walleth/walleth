package org.walleth.functions

import org.walleth.data.tokens.Token
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat

val formatter: DecimalFormat
    get() = DecimalFormat("#.#####")
val roundingFormatter: DecimalFormat
    get() = DecimalFormat("~#.#####")

fun Token.decimalsInZeroes() = "0".repeat (decimals)
fun Token.decimalsAsMultiplicator() = BigDecimal("1"+this.decimalsInZeroes())

fun BigInteger.toValueString(token: Token) = BigDecimal(this).toValueString(token)

fun BigDecimal.toValueString(token: Token): String {
    val inEther = divide(BigDecimal("1" + token.decimalsInZeroes())).stripTrailingZeros()
    if (inEther.scale() < 6) {
        return formatter.format(inEther)
    } else {
        return roundingFormatter.format(inEther)
    }
}
