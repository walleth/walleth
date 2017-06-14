package org.walleth.data

import org.walleth.data.tokens.TokenDescriptor
import java.math.BigInteger

data class BalanceAtBlock(val block: Long, val balance: BigInteger, val tokenDescriptor: TokenDescriptor)