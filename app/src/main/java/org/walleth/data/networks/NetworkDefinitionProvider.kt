package org.walleth.data.networks

import androidx.lifecycle.MutableLiveData
import org.kethereum.model.Address
import org.kethereum.model.ChainId
import org.walleth.data.config.Settings

fun getNetworkDefinitionByChainID(chainID: ChainId) = ALL_NETWORKS.firstOrNull { it.chain.id == chainID }

fun ChainId.findNetworkDefinition() = ALL_NETWORKS.firstOrNull { it.chain.id == this }

const val FAUCET_ADDRESS_TOKEN = "%address%"
fun ChainId.getFaucetURL(address: Address): String? {
    return findNetworkDefinition()
            ?.faucets?.first()
            ?.replace(FAUCET_ADDRESS_TOKEN, address.hex)
}

class NetworkDefinitionProvider(var settings: Settings) : MutableLiveData<NetworkDefinition>() {

    init {
        value = ChainId(settings.chain).findNetworkDefinition() ?: ALL_NETWORKS.first()
    }

    fun setCurrent(value: NetworkDefinition) {
        settings.chain = value.chain.id.value
        setValue(value)
    }

    fun getCurrent() = value!!

}