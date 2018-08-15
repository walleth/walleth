package org.walleth.walletconnect.model

data class Session(val sessionId: String,
                   val domain: String,
                   val dappName: String,
                   val sharedKey: String)