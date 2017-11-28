package org.walleth.functions

import org.walleth.data.tokens.Token
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat

val decimalFormatter = DecimalFormat("#.#####")

fun Token.decimalsInZeroes() = "0".repeat(decimals)
fun Token.decimalsAsMultiplicator() = BigDecimal("1" + this.decimalsInZeroes())

fun BigInteger.toValueString(token: Token) = BigDecimal(this).toValueString(token)

fun BigInteger.toFullValueString(token: Token) = String.format("%f", BigDecimal(this).inETH(token))

fun BigDecimal.inETH(token: Token): BigDecimal = divide(BigDecimal("1" + token.decimalsInZeroes())).stripTrailingZeros()
fun BigDecimal.toValueString(token: Token) = inETH(token).let { valueInETH ->
    decimalFormatter.format(valueInETH).addPrefixOnCondition(prefix = "~", condition = valueInETH.scale() < 6)
}

fun BigDecimal.toFiatValueString()
        =  String.format("%.2f", this).addPrefixOnCondition(prefix = "~", condition = scale() <= 2)

fun String.addPrefixOnCondition(prefix: String, condition: Boolean) = if (condition) this else prefix + this