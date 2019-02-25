package org.walleth.data.tokens

import android.arch.lifecycle.MediatorLiveData
import org.walleth.data.networks.NetworkDefinitionProvider

open class CurrentTokenProvider(val networkDefinitionProvider: NetworkDefinitionProvider,
                                initialValue: Token = getRootTokenForChain(networkDefinitionProvider.getCurrent())) : MediatorLiveData<Token>() {

    init {
        value = initialValue
        addSource(networkDefinitionProvider) {
            it?.let { networkDefinition ->
                if (networkDefinition.chain.id != getCurrent().chain.id) {
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
