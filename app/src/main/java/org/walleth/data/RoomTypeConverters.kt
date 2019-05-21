package org.walleth.data

import androidx.room.TypeConverter
import org.kethereum.model.Address
import org.kethereum.model.ChainId
import java.math.BigInteger
import java.util.*

class RoomTypeConverters {

    /** Address  */

    @TypeConverter
    fun addressFromString(value: String?) = if (value == null) null else Address(value)

    @TypeConverter
    fun addressToString(address: Address?) = address?.hex

    /** Date  */

    @TypeConverter
    fun dateFromLong(value: Long?) = if (value == null) null else Date(value)

    @TypeConverter
    fun dateToLong(date: Date?) = date?.time

    /** BigInteger  */

    @TypeConverter
    fun bigintegerFromByteArray(value: ByteArray?) = if (value == null) null else BigInteger(value)

    @TypeConverter
    fun bigIntegerToByteArray(bigInteger: BigInteger?) = bigInteger?.toByteArray()


    /** List<String> */
    @TypeConverter
    fun stringListFromString(value: String?) = value?.split("%!%")?.filter { it.isNotBlank() }

    @TypeConverter
    fun stringListToString(value: List<String>?) = value?.joinToString("%!%")

    /** List<String> */
    @TypeConverter
    fun longToChainId(value: Long?) = value?.let { ChainId(it) }

    @TypeConverter
    fun chainIdToLong(value: ChainId?) = value?.value
}