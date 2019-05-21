package org.walleth.data.chaininfo

import androidx.room.Embedded
import androidx.room.Entity
import java.math.BigInteger

data class NativeCurrency(val decimals: Int, val name: String, val symbol: String)
@Entity(tableName = "chains", primaryKeys = ["chainId"])
data class ChainInfo(
        val name: String,
        val chainId: BigInteger,
        val networkId: Long,
        val shortName: String,
        val rpc: List<String>,
        val faucets: List<String>,
        val infoURL: String,
        var order: Int?,
        var starred: Boolean,
        var softDeleted: Boolean,
        @Embedded(prefix = "token_")
        val nativeCurrency: NativeCurrency
)