package org.ligi.walleth.data.exchangerate

import java.math.BigDecimal

interface ExchangeRateProvider {
    fun getExChangeRate(name: String) : BigDecimal?
}


