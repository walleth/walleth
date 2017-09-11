package org.walleth.data

import android.arch.persistence.room.TypeConverter
import org.kethereum.model.Address
import org.walleth.data.networks.ChainDefinition
import java.math.BigInteger
import java.util.*

class RoomTypeConverters {

    /** Address  */

    @TypeConverter
    fun fromTimestamp(value: String?) = if (value == null) null else Address(value)

    @TypeConverter
    fun dateToTimestamp(address: Address?) = address?.hex

    /** NetworkDefinition  */

    @TypeConverter
    fun fromNetworkDefinition(value: String)
            = ChainDefinition(java.lang.Long.parseLong(value.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]), "ETH")

    @TypeConverter
    fun toNetworkDefintion(chain: ChainDefinition?) = if (chain == null) null else "ETH:" + chain.id

    /** Date  */

    @TypeConverter
    fun fromTimestamp(value: Long?) = if (value == null) null else Date(value)

    @TypeConverter
    fun dateToTimestamp(date: Date?) = date?.time


    /** BigInteger  */

    @TypeConverter
    fun fromBigInteger(value: String?) = if (value == null) null else BigInteger(value)

    @TypeConverter
    fun bigIntegerToString(bigInteger: BigInteger?) = bigInteger?.toString()
}