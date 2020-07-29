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

interface RPCProvider {
    suspend fun get(): EthereumRPC?
    suspend fun getForChain(chainId: ChainId): EthereumRPC?
}

class RPCProviderImpl(var network: ChainInfoProvider,
                      var appDatabase: AppDatabase,
                      var okHttpClient: OkHttpClient,
                      var settings: Settings) : RPCProvider {

    private fun ChainInfo.get(): BaseEthereumRPC? {


        val transport = if (settings.dappNodeMode == DappNodeMode.ONLY_USE_DAPPNODE) {
            getDappNodeTransport()
        } else {
            val potentialTransport = if (settings.dappNodeMode == DappNodeMode.USE_WHEN_POSSIBLE) {
                getDappNodeTransport()
            } else {
                null
            }

            potentialTransport ?: getMin3BootnNdesByChainId(ChainId(chainId))?.let { bootNodes ->
                MIN3Transport(bootNodes, okHttpClient, debug = settings.logRPCRequests)
            } ?: getRPCEndpoint()?.let {
                HttpTransport(it, okHttpClient, settings.logRPCRequests)
            }
        }

        return transport?.let { nonNullTransport ->
            BaseEthereumRPC(nonNullTransport)
        }
    }

    private fun ChainInfo.getDappNodeTransport() = getDappNodeURL(chainId)?.let {
        HttpTransport(it, okHttpClient, settings.logRPCRequests)
    }


    override suspend fun getForChain(chainId: ChainId) = appDatabase.chainInfo.getByChainId(chainId.value)?.get()

    override suspend fun get(): EthereumRPC? = network.getCurrent()?.get()

}

fun getDappNodeURL(chainId: BigInteger): String? = ChainIdToDappNodeRPC[chainId.toInt()]

val ChainIdToDappNodeRPC = mapOf(
        1 to "http://fullnode.dappnode:8545",
        3 to "http://ropsten.dappnode:8545",
        4 to "http://rinkeby.dappnode:8545",
        5 to "http://goerli-geth.dappnode:8545",
        42 to "http://kovan.dappnode:8545"
)