package org.walleth.data.transactions

import org.threeten.bp.LocalDateTime
import org.walleth.data.WallethAddress
import java.math.BigInteger

data class Transaction(val value: BigInteger,
                       val from: WallethAddress,
                       val to: WallethAddress,

                       val localTime: LocalDateTime = LocalDateTime.now(),
                       var ref: TransactionSource = TransactionSource.WALLETH,
                       var nonce: Long? = null,
                       var error: String? = null,
                       var sigHash: String? = null,
                       var txHash: String? = null
)
