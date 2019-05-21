package org.walleth.data.tokens

import androidx.lifecycle.MediatorLiveData
import org.walleth.data.networks.ChainInfoProvider

open class CurrentTokenProvider(val chainInfoProvider: ChainInfoProvider) : MediatorLiveData<Token>() {

    init {
        addSource(chainInfoProvider) {
            it?.let { chainInfo ->
                if (chainInfo.chainId != value?.chain) {
                    value = chainInfo.getRootToken()
                }
            }
        }
    }

    fun setCurrent(newValue: Token) {
        value = newValue
    }

    fun getCurrent() = value!!
}
