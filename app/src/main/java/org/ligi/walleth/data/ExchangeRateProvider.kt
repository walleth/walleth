package org.ligi.walleth.data

interface ExchangeRateProvider {
    fun getExChangeRate(name: String) : Double?
}


