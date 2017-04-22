package org.ligi.walleth.data

class FixedValueExchangeProvider : ExchangeRateProvider {

    var exchangeRateMap : MutableMap<String,Double> = mutableMapOf( "EUR" to 10.01 )

    override fun getExChangeRate(name: String) = exchangeRateMap[name]

}