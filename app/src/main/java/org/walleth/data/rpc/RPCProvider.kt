package org.walleth.data.rpc

import okhttp3.OkHttpClient
import org.kethereum.rpc.EthereumRPC
import org.kethereum.rpc.HttpEthereumRPC
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.util.getRPCEndpoint

interface RPCProvider {
    fun get(): EthereumRPC
}

class RPCProviderImpl(var network: NetworkDefinitionProvider, var okHttpClient: OkHttpClient) : RPCProvider {

    override fun get(): EthereumRPC = HttpEthereumRPC(network.getCurrent().getRPCEndpoint(), okHttpClient)

}