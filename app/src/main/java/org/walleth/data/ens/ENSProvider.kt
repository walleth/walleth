package org.walleth.data.ens

import org.kethereum.ens.TypedENS
import org.kethereum.model.Address
import org.kethereum.model.ChainId
import org.walleth.data.rpc.RPCProvider

interface ENSProvider {
    fun get(): TypedENS?
}

class ENSProviderImpl(var rpcProvider: RPCProvider) : ENSProvider {

    override fun get(): TypedENS? = rpcProvider.getForChain(ChainId(1))?.let {
        TypedENS(it, Address("0x314159265dd8dbb310642f98f50c066173c1259b"))
    }

}