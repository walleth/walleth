package org.walleth.data.ens

import org.kethereum.ens.ENS_DEFAULT_CONTRACT_ADDRESS
import org.kethereum.ens.TypedENS
import org.kethereum.model.ChainId
import org.walleth.data.rpc.RPCProvider

interface ENSProvider {
    fun get(): TypedENS?
}

class ENSProviderImpl(var rpcProvider: RPCProvider) : ENSProvider {

    override fun get(): TypedENS? = rpcProvider.getForChain(ChainId(1))?.let {
        TypedENS(it, ENS_DEFAULT_CONTRACT_ADDRESS)
    }

}