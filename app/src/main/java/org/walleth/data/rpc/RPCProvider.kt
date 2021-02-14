package org.walleth.data.rpc

import okhttp3.OkHttpClient
import org.kethereum.model.ChainId
import org.kethereum.rpc.BaseEthereumRPC
import org.kethereum.rpc.EthereumRPC
import org.kethereum.rpc.HttpTransport
import org.kethereum.rpc.RPCTransport
import org.kethereum.rpc.min3.MIN3Transport
import org.kethereum.rpc.min3.getMin3BootnNdesByChainId
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.AppDatabase
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.data.config.DappNodeMode
import org.walleth.data.config.Settings
import org.walleth.util.getRPCEndpoint
import java.math.BigInteger

class ConsoleLoggingTransportWrapper(private val transport: RPCTransport) : RPCTransport {
    override fun call(payload: String): String? {
        println("Transport via $transport")
        println("> payload: $payload")
        return transport.call(payload).also {
            println("< response: $it")
        }
    }
}

const val KEY_IN3_RPC = "in3"

// open only for mocking - once switched to mockk we can remove
open class DescribedRPC(transport: RPCTransport, val description : String) : BaseEthereumRPC(transport)

interface RPCProvider {
    suspend fun get(): DescribedRPC?
    suspend fun getForChain(chainId: ChainId): DescribedRPC?
}

class RPCProviderImpl(var network: ChainInfoProvider,
                      var appDatabase: AppDatabase,
                      var okHttpClient: OkHttpClient,
                      var settings: Settings) : RPCProvider {

    private fun ChainInfo.get(): DescribedRPC? {


        if (settings.dappNodeMode == DappNodeMode.ONLY_USE_DAPPNODE) {
            return getDappNodeDescribedRPC()
        } else {
            if (settings.dappNodeMode == DappNodeMode.USE_WHEN_POSSIBLE) {
                return getDappNodeDescribedRPC()
            }

            getMin3BootnNdesByChainId(ChainId(chainId))?.let { bootNodes ->
                return DescribedRPC(MIN3Transport(bootNodes, okHttpClient, debug = settings.logRPCRequests), "MIN3/TincubETH chainId $chainId")
            } ?: getRPCEndpoint()?.let {
                return DescribedRPC(HttpTransport(it, okHttpClient, settings.logRPCRequests), "RPC $it")
            }
        }
        return null
    }

    private fun ChainInfo.getDappNodeDescribedRPC() = getDappNodeURL(chainId)?.let {
        DescribedRPC(HttpTransport(it, okHttpClient, settings.logRPCRequests), it )
    }


    override suspend fun getForChain(chainId: ChainId) = appDatabase.chainInfo.getByChainId(chainId.value)?.get()

    override suspend fun get(): DescribedRPC? = network.getCurrent()?.get()

}

fun getDappNodeURL(chainId: BigInteger): String? = ChainIdToDappNodeRPC[chainId.toInt()]

val ChainIdToDappNodeRPC = mapOf(
        1 to "http://fullnode.dappnode:8545",
        3 to "http://ropsten.dappnode:8545",
        4 to "http://rinkeby.dappnode:8545",
        5 to "http://goerli-geth.dappnode:8545",
        42 to "http://kovan.dappnode:8545"
)