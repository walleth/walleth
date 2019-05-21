package org.walleth.data.rpc

import okhttp3.OkHttpClient
import org.kethereum.rpc.EthereumRPC
import org.kethereum.rpc.HttpEthereumRPC
import org.walleth.data.networks.ChainInfoProvider
import org.walleth.util.getRPCEndpoint

interface RPCProvider {
    fun get(): EthereumRPC?
}

class RPCProviderImpl(var network: ChainInfoProvider, var okHttpClient: OkHttpClient) : RPCProvider {

    override fun get(): EthereumRPC? = network.getCurrent()?.getRPCEndpoint()?.let {
        HttpEthereumRPC(it, okHttpClient)
    }

}