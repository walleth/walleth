package org.walleth.infrastructure

import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.Token

class FixedCurrentTokenProvider(var token: Token) : CurrentTokenProvider {
    override suspend fun getFlow() = MutableStateFlow(token)

    override suspend fun setCurrent(newValue: Token) {
        token = newValue
    }

    override suspend fun getCurrent() = token
}

fun setCurrentToken(token: Token) {
    loadKoinModules(
            listOf(module(override = true) {
                single<CurrentTokenProvider> { FixedCurrentTokenProvider(token) }
            })
    )
}