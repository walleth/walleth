package org.walleth.data.networks

import android.arch.lifecycle.MutableLiveData
import org.walleth.data.config.Settings

fun getNetworkDefinitionByChainID(chainID: Long) = ALL_NETWORKS.firstOrNull { it.chain.id == chainID }

class NetworkDefinitionProvider(var settings: Settings) : MutableLiveData<NetworkDefinition>() {

    init {
        value = ALL_NETWORKS.firstOrNull { it.chain.id == settings.chain } ?: ALL_NETWORKS.first()
    }

    fun setCurrent(value: NetworkDefinition) {
        settings.chain = value.chain.id
        setValue(value)
    }

    fun getCurrent() = value!!
}