package org.walleth.functions

import org.walleth.data.tokens.Token
import java.math.BigDecimal
import java.math.BigInteger

fun Token.decimalsInZeroes() = "0".repeat(decimals)
fun Token.decimalsAsMultiplicator() = BigDecimal("1" + this.decimalsInZeroes())

fun BigInteger.toValueString(token: Token) = BigDecimal(this).toValueString(token)

fun BigInteger.toFullValueString(token: Token) = String.format("%f", BigDecimal(this).applyTokenDecimals(token))

fun BigDecimal.applyTokenDecimals(token: Token): BigDecimal = divide(BigDecimal("1" + token.decimalsInZeroes())).stripTrailingZeros()
fun BigDecimal.toValueString(token: Token) = applyTokenDecimals(token).let { valueInETH ->
    sixDigitDecimalFormat.format(valueInETH)
            .addPrefixOnCondition(prefix = "~", condition = valueInETH.scale() <= 6)
            .stripTrailingZeros()
}

fun BigDecimal.toFiatValueString()
        = twoDigitDecimalFormat.format(this)
        .addPrefixOnCondition(prefix = "~", condition = scale() <= 2)
        .stripTrailingZeros()
        .adjustToMonetary2DecimalsWhenNeeded()

fun String.addPrefixOnCondition(prefix: String, condition: Boolean) = if (condition) this else prefix + this