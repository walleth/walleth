package org.walleth.testdata

import org.walleth.data.exchangerate.BaseExchangeProvider
import org.walleth.data.exchangerate.FiatInfo
import org.threeten.bp.LocalTime
import java.math.BigDecimal

class FixedValueExchangeProvider : BaseExchangeProvider() {

    override val fiatInfoMap by lazy {
        mutableMapOf(
                "EUR" to FiatInfo("EUR", lastUpdated = LocalTime.now().minusHours(10), exchangeRate = BigDecimal("57.09")),
                "NZD" to FiatInfo("NZD", lastUpdated = LocalTime.now().minusHours(15), exchangeRate = BigDecimal("97.53")),
                "CAD" to FiatInfo("CAD", lastUpdated = LocalTime.now().minusHours(20), exchangeRate = BigDecimal("86.89")),
                "USD" to FiatInfo("USD", lastUpdated = LocalTime.now().minusHours(23), exchangeRate = BigDecimal("63.12"))
        )
    }

}