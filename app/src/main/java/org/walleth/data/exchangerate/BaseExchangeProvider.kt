package org.walleth.data.exchangerate

import org.walleth.data.ETH_IN_WEI
import java.math.BigDecimal
import java.math.BigInteger

abstract class BaseExchangeProvider : ExchangeRateProvider {

    open protected val fiatInfoMap = mutableMapOf<String, FiatInfo>()

    override fun addFiat(name: String) {
        fiatInfoMap.put(name, FiatInfo(name))
    }

    override fun getAvailableFiatInfoMap() = fiatInfoMap

    override fun getExChangeRate(name: String) = fiatInfoMap[name]?.exchangeRate

    override fun getConvertedValue(value: BigInteger?, currencySymbol: String) = if (value == null || getExChangeRate(currencySymbol) == null) {
        null
    } else {
        val exchangeRate = getExChangeRate(currencySymbol)!!
        val divided = BigDecimal(value).divide(BigDecimal(ETH_IN_WEI))
        exchangeRate.times(divided).stripTrailingZeros()
    }

}