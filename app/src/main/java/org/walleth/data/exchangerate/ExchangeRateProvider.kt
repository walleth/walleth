package org.walleth.data.exchangerate

import java.math.BigDecimal
import java.math.BigInteger

interface ExchangeRateProvider {
    fun getExChangeRate(name: String): BigDecimal?
    fun getAvailableFiatInfoMap(): Map<String, FiatInfo>
    fun addFiat(name: String)
    fun convertToFiat(value: BigInteger?, currencySymbol: String): BigDecimal?
    fun convertFromFiat(value: BigDecimal?, currencySymbol: String): BigInteger?
}


