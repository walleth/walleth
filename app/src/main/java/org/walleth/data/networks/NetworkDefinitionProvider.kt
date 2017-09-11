package org.walleth.data.networks

import android.arch.lifecycle.MutableLiveData

val AllNetworkDefinitions = mutableListOf(RinkebyNetworkDefinition(), MainnetNetworkDefinition(), RopstenNetworkDefinition())

fun getNetworkDefinitionByChainID(chainID: Long) = AllNetworkDefinitions.firstOrNull{ it.chain.id == chainID }

class NetworkDefinitionProvider : MutableLiveData<NetworkDefinition>() {

    init {
        value = AllNetworkDefinitions.first()
    }

    fun setCurrent(value: NetworkDefinition) {
        setValue(value)
    }

    fun getCurrent() = value!!
}