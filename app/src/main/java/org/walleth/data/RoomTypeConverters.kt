package org.walleth.data

import android.arch.persistence.room.TypeConverter
import org.kethereum.model.Address
import org.kethereum.model.ChainDefinition
import org.kethereum.model.ChainId
import org.walleth.data.transactions.TransactionSource
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

    /** NetworkDefinition  */

    @TypeConverter
    fun fromNetworkDefinition(value: String): ChainDefinition {
        val split = value.split(":")
        return ChainDefinition(ChainId(split.last().toLong()), split.first())
    }

    @TypeConverter
    fun toNetworkDefinition(chain: ChainDefinition?) = chain?.toString()

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


    /** TransactionSource */

    @TypeConverter
    fun fromTransactionSourceString(value: String) = TransactionSource.valueOf(value)

    @TypeConverter
    fun toTransactionSourceString(value: TransactionSource) = value.toString()


    /** List<Byte> **/

    @TypeConverter
    fun fromByteArrayString(value: String) = value.hexToByteArray().toList()

    @TypeConverter
    fun toTransactionSourceString(value: List<Byte>) = value.toByteArray().toHexString()

}