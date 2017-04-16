package org.ligi.walleth.data

class FixedValueExchangeProvider : ExchangeRateProvider {

    override fun getExChangeRate(name: String) = 10.08

}