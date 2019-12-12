package org.walleth.data.chaininfo

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

@Parcelize
data class NativeCurrency(
        val symbol: String,
        val name: String = symbol,
        val decimals: Int = 18
) : Parcelable

@Entity(tableName = "chains", primaryKeys = ["chainId"])
@Parcelize
data class ChainInfo(
        val name: String,
        val chainId: BigInteger,
        val networkId: Long,
        val shortName: String,
        val rpc: List<String> = emptyList(),
        val faucets: List<String> = emptyList(),
        val infoURL: String = "",
        var order: Int? = null,
        var starred: Boolean = false,
        var softDeleted: Boolean = false,
        @Embedded(prefix = "token_")
        val nativeCurrency: NativeCurrency
) : Parcelable