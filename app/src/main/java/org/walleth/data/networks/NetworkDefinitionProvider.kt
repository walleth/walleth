package org.walleth.data.networks

import android.arch.lifecycle.MutableLiveData
import org.walleth.data.config.Settings

val AllNetworkDefinitions = mutableListOf(RinkebyNetworkDefinition(), MainNetNetworkDefinition(), RopstenNetworkDefinition())

fun getNetworkDefinitionByChainID(chainID: Long) = AllNetworkDefinitions.firstOrNull { it.chain.id == chainID }

class NetworkDefinitionProvider(var settings: Settings) : MutableLiveData<NetworkDefinition>() {

    init {
        value = AllNetworkDefinitions.firstOrNull { it.chain.id == settings.chain } ?: AllNetworkDefinitions.first()
    }

    fun setCurrent(value: NetworkDefinition) {
        settings.chain = value.chain.id
        setValue(value)
    }

    fun getCurrent() = value!!
}