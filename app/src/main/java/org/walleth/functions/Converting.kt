package org.walleth.functions

import org.ethereum.geth.BigInt
import org.walleth.khex.prepend0xPrefix
import java.math.BigInteger

fun BigInteger.toGethInteger() = BigInt(toLong())
fun BigInteger.toHexString() = this.toString(16).prepend0xPrefix()
