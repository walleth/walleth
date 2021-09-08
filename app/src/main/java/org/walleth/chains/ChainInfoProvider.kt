package org.walleth.chains

import android.content.res.AssetManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types.newParameterizedType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.kethereum.model.Address
import org.kethereum.model.ChainId
import org.walleth.data.*
import org.walleth.data.addresses.AccountKeySpec
import org.walleth.data.addresses.toJSON
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.data.config.Settings
import org.walleth.data.tokens.getRootToken
import java.math.BigInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


fun <T> suspendBlockingLazy(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        initializer: () -> T
): SuspendLazy<T> = SuspendLazyBlockingImpl(dispatcher, initializer)

fun <T> CoroutineScope.suspendLazy(
        context: CoroutineContext = EmptyCoroutineContext,
        initializer: suspend CoroutineScope.() -> T
): SuspendLazy<T> = SuspendLazySuspendingImpl(this, context, initializer)

interface SuspendLazy<out T> {
    suspend operator fun invoke(): T
}

private class SuspendLazyBlockingImpl<out T>(
        private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
        initializer: () -> T
) : SuspendLazy<T> {
    private val lazyValue = lazy(initializer)
    override suspend operator fun invoke(): T = with(lazyValue) {
        if (isInitialized()) value else withContext(dispatcher) { value }
    }
}

private class SuspendLazySuspendingImpl<out T>(
        coroutineScope: CoroutineScope,
        context: CoroutineContext,
        initializer: suspend CoroutineScope.() -> T
) : SuspendLazy<T> {
    private val deferred = coroutineScope.async(context, start = CoroutineStart.LAZY, block = initializer)
    override suspend operator fun invoke(): T = deferred.await()
}

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
                        val keyStore: org.kethereum.keystore.api.KeyStore,
                        private val moshi: Moshi,
                        private val assetManager: AssetManager) {

    private val flow = GlobalScope.suspendLazy {
        getInitial() // we need to make sure chains are initialized before we init tokens
        initTokens(settings, assetManager, appDatabase)

        GlobalScope.launch(Dispatchers.Default) {
            if (settings.dataVersion < 3) {
                val all = appDatabase.chainInfo.getAll()
                var currentMin = all.filter { it.order != null }.minByOrNull { it.order!! }?.order ?: 0
                all.forEach {
                    if (it.order == null) {
                        it.order = currentMin
                    }
                    currentMin -= 10
                }
                appDatabase.chainInfo.upsert(all)
            }
            if (settings.dataVersion < 1) {
                appDatabase.addressBook.all().forEach {
                    if (it.keySpec == null || it.keySpec?.isBlank() == true) {
                        val type = if (keyStore.hasKeyForForAddress(it.address)) ACCOUNT_TYPE_BURNER else ACCOUNT_TYPE_WATCH_ONLY
                        it.keySpec = AccountKeySpec(type).toJSON()
                        appDatabase.addressBook.upsert(it)
                    } else if (it.keySpec?.startsWith("m") == true) {
                        it.keySpec = AccountKeySpec(ACCOUNT_TYPE_TREZOR, derivationPath = it.keySpec).toJSON()
                        appDatabase.addressBook.upsert(it)
                    }
                }
            }
            settings.dataVersion = 4
        }

        MutableStateFlow(getInitial())
    }

    suspend fun getFlow() = flow()

    private suspend fun getInitial(): ChainInfo = appDatabase.chainInfo.getByChainId(settings.chain.toBigInteger())
            ?: appDatabase.chainInfo.getByChainId(BigInteger.valueOf(5L))
            ?: appDatabase.chainInfo.getAll().firstOrNull()
            ?: appDatabase.chainInfo.upsert(assetManager.loadInitChains(moshi)).let {
                getInitial()
            }

    suspend fun setCurrent(value: ChainInfo) {
        settings.chain = value.chainId.toLong()
        flow().emit(value)
    }

    suspend fun getCurrent() = flow().value
    suspend fun getCurrentChainId() = ChainId(flow().value.chainId)

}