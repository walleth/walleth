package org.walleth.data.tokens

import androidx.lifecycle.MediatorLiveData
import org.walleth.data.networks.NetworkDefinitionProvider

open class CurrentTokenProvider(val networkDefinitionProvider: NetworkDefinitionProvider) : MediatorLiveData<Token>() {

    init {
        addSource(networkDefinitionProvider) {
            it?.let { networkDefinition ->
                if (networkDefinition.chain.id.value != value?.chain) {
                    value = getRootTokenForChain(networkDefinition)
                }
            }
        }
    }

    fun setCurrent(newValue: Token) {
        value = newValue
    }

    fun getCurrent() = value!!
}
