package org.walleth.chains

import android.content.res.AssetManager
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types.newParameterizedType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kethereum.model.Address
import org.kethereum.model.ChainId
import org.walleth.data.AppDatabase
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.data.config.Settings
import java.math.BigInteger

private const val FAUCET_ADDRESS_TOKEN = "\${ADDRESS}"
fun ChainInfo.getFaucetURL(address: Address) = getFaucetWithAddressSupport()?.replace(FAUCET_ADDRESS_TOKEN, address.hex) ?: faucets.firstOrNull()
fun ChainInfo.hasFaucetWithAddressSupport() = getFaucetWithAddressSupport() != null
private fun ChainInfo.getFaucetWithAddressSupport() = faucets.firstOrNull { it.contains(FAUCET_ADDRESS_TOKEN) }

fun AssetManager.loadInitChains(moshi: Moshi): List<ChainInfo> {
    val chainsJSON = open("init_chains.json").reader().readText()
    return moshi.deSerialize(chainsJSON)
}

val orderMap = mapOf(1 to 1337, 100 to 88, 61 to 61, 5 to 42, 6 to 6, 4 to 4, 3 to 3, 977 to 2, 77 to 1)

fun Moshi.deSerialize(chainsJSON: String): List<ChainInfo> {
    val adapter: JsonAdapter<List<ChainInfo>> = adapter(newParameterizedType(List::class.java, ChainInfo::class.java))
    val list = adapter.fromJson(chainsJSON) ?: emptyList()
    list.forEach {
        val chainId = it.chainId.toInt()
        if (orderMap.containsKey(chainId)) {
            it.order = orderMap[chainId]
        }
    }
    return list
}

class ChainInfoProvider(val settings: Settings,
                        val appDatabase: AppDatabase,
                        private val moshi: Moshi,
                        private val assetManager: AssetManager) : MutableLiveData<ChainInfo>() {

    init {
        GlobalScope.launch(Dispatchers.Default) {
            postValue(getInitial())
        }
    }

    private suspend fun getInitial(): ChainInfo = appDatabase.chainInfo.getByChainId(settings.chain.toBigInteger())
            ?: appDatabase.chainInfo.getByChainId(BigInteger.valueOf(5L))
            ?: appDatabase.chainInfo.getAll().firstOrNull()
            ?: appDatabase.chainInfo.upsert(assetManager.loadInitChains(moshi)).let {
                getInitial()
            }

    fun setCurrent(value: ChainInfo) {
        settings.chain = value.chainId.toLong()
        setValue(value)
    }

    fun getCurrent() = value
    fun getCurrentChainId() = ChainId(value!!.chainId)

}