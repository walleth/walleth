package org.walleth.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

// ENGLISH is used until Android O becomes the minSDK or the support-lib fixes this problem:
// https://stackoverflow.com/questions/3821539/decimal-separator-comma-with-numberdecimal-inputtype-in-edittext
val inputDecimalFormat = (NumberFormat.getInstance(Locale.ENGLISH) as DecimalFormat).apply {
    isParseBigDecimal = true
}

private fun getDecimalFormatUS(): DecimalFormat = NumberFormat.getInstance(Locale.ENGLISH) as DecimalFormat

val sixDigitDecimalFormat = getDecimalFormat(6)
val twoDigitDecimalFormat = getDecimalFormat(2)

private val endingWithOneNumber = "^.*\\.[0-9]$".toRegex()
fun String.stripTrailingZeros() = trimEnd('0').trimEnd('.')
fun String.adjustToMonetary2DecimalsWhenNeeded() = if (endingWithOneNumber.matches(this)) "${this}0" else this

private fun getDecimalFormat(decimals: Int) = getDecimalFormatUS().apply {
    applyPattern("#0." + "0".repeat(decimals))
    isGroupingUsed = false
}

fun String.asBigDecimal() = inputDecimalFormat.parseObject(this).let {

    when (it) {
        is Double -> BigDecimal(it)
        else -> it as BigDecimal
    }

}