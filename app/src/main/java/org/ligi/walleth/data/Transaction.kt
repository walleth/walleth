package org.ligi.walleth.data

import org.threeten.bp.LocalDateTime
import java.math.BigInteger

enum class Source {
    WALLETH,
    WALLETH_PROCESSED,
    GETH,
    ETHERSCAN
}

data class Transaction(val value: BigInteger,
                       val from: WallethAddress,
                       val to: WallethAddress,
                       val localTime: LocalDateTime = LocalDateTime.now(),
                       var ref: Source = Source.WALLETH,
                       var error: String? = null,
                       var sigHash: String? = null,
                       var txHash: String? = null
)
