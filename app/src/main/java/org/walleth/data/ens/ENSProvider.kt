package org.walleth.data.ens

import org.kethereum.ens.ENS
import org.kethereum.model.ChainId
import org.walleth.data.rpc.RPCProvider

interface ENSProvider {
    suspend fun get(): ENS?
}

class ENSProviderImpl(var rpcProvider: RPCProvider) : ENSProvider {

    override suspend fun get(): ENS? = rpcProvider.getForChain(ChainId(1))?.let {
        ENS(it)
    }

}