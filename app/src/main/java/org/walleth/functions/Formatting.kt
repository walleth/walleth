package org.walleth.functions

import org.walleth.data.tokens.Token
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat

val formatter = DecimalFormat("#.#####")

fun Token.decimalsInZeroes() = "0".repeat (decimals)
fun Token.decimalsAsMultiplicator() = BigDecimal("1"+this.decimalsInZeroes())

fun BigInteger.toValueString(token: Token) = BigDecimal(this).toValueString(token)

fun BigDecimal.toValueString(token: Token): String {
    val inEther = divide(BigDecimal("1" + token.decimalsInZeroes())).stripTrailingZeros()
    val formatted = formatter.format(inEther);
    if (inEther.scale() < 6) {
        return formatted
    } else {
        return "~$formatted"
    }
}
