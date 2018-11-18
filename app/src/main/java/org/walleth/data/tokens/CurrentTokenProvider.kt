package org.walleth.data.tokens

import android.arch.lifecycle.MutableLiveData
import org.walleth.data.networks.NetworkDefinitionProvider

open class CurrentTokenProvider(val networkDefinitionProvider: NetworkDefinitionProvider,
                                initialValue: Token = getEthTokenForChain(networkDefinitionProvider.getCurrent())): MutableLiveData<Token>() {

    init {
        value = initialValue
    }

    fun setCurrent(value: Token) {
        setValue(value)
    }

    fun getCurrent() = value!!
}
