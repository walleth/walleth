package org.walleth.data.rpc

import okhttp3.OkHttpClient
import org.kethereum.model.ChainId
import org.kethereum.rpc.BaseEthereumRPC
import org.kethereum.rpc.ConsoleLoggingTransportWrapper
import org.kethereum.rpc.EthereumRPC
import org.kethereum.rpc.HttpTransport
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.AppDatabase
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.util.getRPCEndpoint

interface RPCProvider {
    suspend fun get(): EthereumRPC?
    suspend fun getForChain(chainId: ChainId): EthereumRPC?
}

class RPCProviderImpl(var network: ChainInfoProvider,
                      var appDatabase: AppDatabase,
                      var okHttpClient: OkHttpClient) : RPCProvider {

    private fun ChainInfo.get() = this.getRPCEndpoint()?.let {
        BaseEthereumRPC(ConsoleLoggingTransportWrapper(HttpTransport(it, okHttpClient)))
    }

    override suspend fun getForChain(chainId: ChainId) = appDatabase.chainInfo.getByChainId(chainId.value)?.get()

    override suspend fun get(): EthereumRPC? = network.getCurrent()?.get()

}