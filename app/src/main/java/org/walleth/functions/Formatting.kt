package org.walleth.functions

import org.walleth.data.ETH_IN_WEI
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

fun BigInteger.toEtherValueString(): String {
    val inEther = BigDecimal(this).divide(BigDecimal(ETH_IN_WEI))
    return inEther.round(MathContext(4, RoundingMode.FLOOR)).stripTrailingZeros().toString()
}
