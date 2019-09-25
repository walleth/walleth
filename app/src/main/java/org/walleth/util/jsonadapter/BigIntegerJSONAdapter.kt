package org.walleth.util.jsonadapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.math.BigInteger

class BigIntegerJSONAdapter {
    @ToJson
    internal fun toJson(bigInteger: BigInteger): String {
        return bigInteger.toString()
    }

    @FromJson
    internal fun fromJson(bigInteger: String): BigInteger {
        return BigInteger(bigInteger)
    }
}