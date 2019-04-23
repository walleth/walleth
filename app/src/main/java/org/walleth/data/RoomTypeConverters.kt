package org.walleth.data

import androidx.room.TypeConverter
import org.kethereum.model.Address
import java.math.BigInteger
import java.util.*

class RoomTypeConverters {

    /** Address  */

    @TypeConverter
    fun fromTimestamp(value: String?) = if (value == null) null else Address(value)

    @TypeConverter
    fun dateToTimestamp(address: Address?) = address?.hex

    /** Date  */

    @TypeConverter
    fun fromTimestamp(value: Long?) = if (value == null) null else Date(value)

    @TypeConverter
    fun dateToTimestamp(date: Date?) = date?.time


    /** BigInteger  */

    @TypeConverter
    fun fromBigInteger(value: ByteArray?) = if (value == null) null else BigInteger(value)

    @TypeConverter
    fun bigIntegerToString(bigInteger: BigInteger?) = bigInteger?.toByteArray()

}