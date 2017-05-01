package org.walleth.data.exchangerate

import org.threeten.bp.LocalTime
import java.math.BigDecimal

data class FiatInfo(val symbol: String,
                    var note: String? = null,
                    var lastUpdated: LocalTime? = null,
                    val exchangeRate: BigDecimal? = null)