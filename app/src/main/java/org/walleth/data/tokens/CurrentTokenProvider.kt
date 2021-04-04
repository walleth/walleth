package org.walleth.data.tokens

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.walleth.chains.ChainInfoProvider
import org.walleth.chains.suspendLazy
import org.walleth.data.chaininfo.ChainInfo

interface CurrentTokenProvider {
    suspend fun getFlow(): Flow<Token>

    suspend fun setCurrent(newValue: Token)

    suspend fun getCurrent(): Token
}

open class CurrentTokenProviderImpl(val chainInfoProvider: ChainInfoProvider) : CurrentTokenProvider {

    private val flow = GlobalScope.suspendLazy {
        MutableStateFlow(chainInfoProvider.getCurrent().getRootToken()).also {
            GlobalScope.launch {
                chainInfoProvider.getFlow().collect { chainInfo ->
                    it.emit(chainInfo.getRootToken())
                }
            }
        }
    }

    override suspend fun getFlow() = flow()

    override suspend fun setCurrent(newValue: Token) {
        flow().emit(newValue)
    }

    override suspend fun getCurrent() = flow().value

}
