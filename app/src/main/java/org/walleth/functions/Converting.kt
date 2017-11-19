package org.walleth.functions

import org.ethereum.geth.BigInt
import org.walleth.data.tokens.Token
import org.walleth.khex.prepend0xPrefix
import java.math.BigDecimal
import java.math.BigInteger

fun BigInteger.toGethInteger() = BigInt(toLong())
fun BigInteger.toHexString() = this.toString(16).prepend0xPrefix()

fun String.extractValueForToken(token: Token) = BigDecimal(this).multiply(token.decimalsAsMultiplicator()).toBigInteger()