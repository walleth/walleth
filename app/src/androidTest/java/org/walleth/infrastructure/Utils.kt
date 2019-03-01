package org.walleth.infrastructure

import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.loadKoinModules
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