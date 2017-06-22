package org.walleth.functions

import org.ethereum.geth.BigInt
import java.math.BigInteger

fun BigInteger.toGethInteger() = BigInt(toLong())