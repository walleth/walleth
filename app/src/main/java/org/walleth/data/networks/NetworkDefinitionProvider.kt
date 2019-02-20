package org.walleth.data.networks

import android.arch.lifecycle.MutableLiveData
import org.kethereum.model.ChainId
import org.walleth.data.config.Settings

fun getNetworkDefinitionByChainID(chainID: ChainId) = ALL_NETWORKS.firstOrNull { it.chain.id == chainID }

class NetworkDefinitionProvider(var settings: Settings) : MutableLiveData<NetworkDefinition>() {

    init {
        value = ALL_NETWORKS.firstOrNull { it.chain.id.value == settings.chain } ?: ALL_NETWORKS.first()
    }

    fun setCurrent(value: NetworkDefinition) {
        settings.chain = value.chain.id.value
        setValue(value)
    }

    fun getCurrent() = value!!
}