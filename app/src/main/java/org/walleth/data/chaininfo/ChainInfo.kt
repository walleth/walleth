package org.walleth.data.chaininfo

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.android.parcel.Parcelize
import org.bouncycastle.math.raw.Nat
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
) : ListItem, Parcelable1 {
    companion object {
        fun from(rpcChainInfoParam: AddEthereumChainParameter): ChainInfo {
            return ChainInfo(
                networkId = rpcChainInfoParam.chainId.toLong(),
                name = rpcChainInfoParam.chainName,
                chainId = rpcChainInfoParam.chainId.toBigInteger(),
                shortName = rpcChainInfoParam.chainName,
                nativeCurrency = rpcChainInfoParam.nativeCurrency
            )
        }
    }
}

//        interface AddEthereumChainParameter {
//                chainId: string; // A 0x-prefixed hexadecimal string
//                chainName: string;
//                nativeCurrency: {
//                        name: string;
//                        symbol: string; // 2-6 characters long
//                        decimals: 18;
//                };
//                rpcUrls: string[];
//                blockExplorerUrls?: string[];
//                iconUrls?: string[]; // Currently ignored.
//        }

data class AddEthereumChainParameter(
    val chainId: String,
    val chainName: String,
    val nativeCurrency: NativeCurrency,
    val rpcUrls: List<String>,
    val blockExplorerUrls: List<String>?,
    val iconUrls: List<String>?
) {
    companion object {
        fun fromParams(params: String, moshi: Moshi): List<AddEthereumChainParameter> {
            val adapter: JsonAdapter<List<AddEthereumChainParameter>> =
                moshi.adapter(Types.newParameterizedType(List::class.java, AddEthereumChainParameter::class.java))
            return adapter.fromJson(params) ?: emptyList()
        }
    }
}
