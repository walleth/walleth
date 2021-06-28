package org.walleth.data.chaininfo

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import kotlinx.android.parcel.Parcelize
import org.walleth.enhancedlist.ListItem
import java.math.BigInteger
import android.os.Parcelable as Parcelable1

@Parcelize
data class NativeCurrency(
        val symbol: String,
        val name: String = symbol,
        val decimals: Int = 18
) : Parcelable1

@Entity(tableName = "chains", primaryKeys = ["chainId"])
@Parcelize
data class ChainInfo(
        override val name: String,
        val chainId: BigInteger,
        val networkId: Long,
        val shortName: String,
        val rpc: List<String> = emptyList(),
        val faucets: List<String> = emptyList(),
        val infoURL: String = "",
        var order: Int? = null,
        var starred: Boolean = false,

        var useEIP1559: Boolean = false,

        @ColumnInfo(name = "softDeleted")
        override var deleted: Boolean = false,

        @Embedded(prefix = "token_")
        val nativeCurrency: NativeCurrency
) : ListItem, Parcelable1