package org.walleth.data.tokens

import androidx.room.ColumnInfo
import androidx.room.Entity
import org.kethereum.model.Address
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.enhancedlist.ListItem
import java.math.BigInteger

fun Token.isRootToken() = address.hex == "0x0"

fun ChainInfo.getRootToken() = Token(
        symbol = nativeCurrency.symbol,
        name = nativeCurrency.name,
        decimals = nativeCurrency.decimals,
        address = Address("0x0"),
        chain = chainId,
        deleted = false,
        starred = false,
        fromUser = false,
        order = 0
)

@Entity(tableName = "tokens", primaryKeys = ["address", "chain"])
data class Token(
        override val name: String,
        val symbol: String,
        val address: Address,
        val decimals: Int,
        val chain: BigInteger,

        @ColumnInfo(name = "softDeleted")
        override var deleted: Boolean = false,

        val starred: Boolean,
        val fromUser: Boolean,
        val order: Int
) : ListItem