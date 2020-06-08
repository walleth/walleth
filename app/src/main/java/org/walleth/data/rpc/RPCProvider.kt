package org.walleth.data.rpc

import android.content.Context
import okhttp3.OkHttpClient
import org.kethereum.model.ChainId
import org.kethereum.rpc.BaseEthereumRPC
import org.kethereum.rpc.ConsoleLoggingTransportWrapper
import org.kethereum.rpc.EthereumRPC
import org.kethereum.rpc.HttpTransport
import org.kethereum.rpc.min3.MIN3Transport
import org.kethereum.rpc.min3.getMin3BootnNdesByChainId
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.AppDatabase
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.data.config.Settings
import org.walleth.util.getRPCEndpoint

const val KEY_IN3_RPC = "in3"

interface RPCProvider {
    suspend fun get(): EthereumRPC?
    suspend fun getForChain(chainId: ChainId): EthereumRPC?
}

class RPCProviderImpl(val context: Context,
                      var network: ChainInfoProvider,
                      var appDatabase: AppDatabase,
                      var okHttpClient: OkHttpClient,
                      var settings: Settings) : RPCProvider {

    private fun ChainInfo.get(): BaseEthereumRPC? {


        val transport = getMin3BootnNdesByChainId(ChainId(chainId))?.let { bootNodes ->
            MIN3Transport(bootNodes, okHttpClient)
        } ?: getRPCEndpoint()?.let {
            HttpTransport(it, okHttpClient)
        }

        return transport?.let { nonNullTransport ->
            BaseEthereumRPC(if (settings.logRPCRequests) ConsoleLoggingTransportWrapper(nonNullTransport) else nonNullTransport)
        }
    }


    override suspend fun getForChain(chainId: ChainId) = appDatabase.chainInfo.getByChainId(chainId.value)?.get()

    override suspend fun get(): EthereumRPC? = network.getCurrent()?.get()

}