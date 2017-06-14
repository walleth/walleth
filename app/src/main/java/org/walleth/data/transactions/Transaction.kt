package org.walleth.data.transactions

import org.threeten.bp.LocalDateTime
import org.walleth.data.DEFAULT_GAS_LIMIT
import org.walleth.data.DEFAULT_GAS_PRICE
import org.walleth.data.WallethAddress
import java.math.BigInteger

data class Transaction(val value: BigInteger,
                       val from: WallethAddress,
                       val to: WallethAddress,

                       val localTime: LocalDateTime = LocalDateTime.now(),
                       var ref: TransactionSource = TransactionSource.WALLETH,
                       var nonce: Long? = null,
                       var gasPrice: BigInteger = DEFAULT_GAS_PRICE,
                       var gasLimit: BigInteger = DEFAULT_GAS_LIMIT,
                       var error: String? = null,
                       var sigHash: String? = null,
                       var needsSigningConfirmation: Boolean = false,
                       var txHash: String? = null,
                       var txRLP: List<Byte>? = null,
                       var signedRLP: List<Byte>? = null,
                       var input: List<Byte> = emptyList(),
                       var eventLog: String? = null)
