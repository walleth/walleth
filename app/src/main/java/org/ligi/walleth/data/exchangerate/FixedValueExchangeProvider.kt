package org.ligi.walleth.data.exchangerate

import java.math.BigDecimal

class FixedValueExchangeProvider : ExchangeRateProvider {

    var exchangeRateMap : MutableMap<String,BigDecimal> = mutableMapOf( "EUR" to BigDecimal("42.07") )

    override fun getExChangeRate(name: String) = exchangeRateMap[name]

}