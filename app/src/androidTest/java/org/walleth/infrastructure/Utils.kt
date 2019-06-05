package org.walleth.infrastructure

import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.Token

fun setCurrentToken(token: Token) {
    loadKoinModules(
            listOf(module(override = true) {
                single { CurrentTokenProvider(get()).apply {
                    setCurrent(token)
                } }
            })
    )
}