package org.ligi.ewallet.data

interface ExchangeRateProvider {
    fun getExChangeRate(name: String) : Double?
}


