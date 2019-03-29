package org.walleth.data

import android.arch.persistence.room.TypeConverter
import org.kethereum.model.Address
import org.walleth.khex.hexToByteArray
import org.walleth.khex.toHexString
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
    fun fromBigInteger(value: String?) = if (value == null) null else BigInteger(value)

    @TypeConverter
    fun bigIntegerToString(bigInteger: BigInteger?) = bigInteger?.toString()

    /** List<Byte> **/

    @TypeConverter
    fun fromByteArrayString(value: String) = value.hexToByteArray().toList()

    @TypeConverter
    fun toTransactionSourceString(value: List<Byte>) = value.toByteArray().toHexString()

}