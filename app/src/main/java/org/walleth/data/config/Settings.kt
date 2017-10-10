package org.walleth.data.config

interface Settings {
    var currentFiat: String
    var startupWarningDone: Boolean

    var chain: Long
    var accountAddress: String?

    var addressInitVersion: Int
    var tokensInitVersion: Int

    var currentGoVerbosity: Int

    fun isLightClientWanted(): Boolean
    fun getNightMode(): Int
    fun getStatsName(): String
}
