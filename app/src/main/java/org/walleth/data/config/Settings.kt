package org.walleth.data.config

interface Settings {
    var currentFiat: String
    var startupWarningDone: Boolean

    var chain: Long
    var accountAddress: String?
    var addressBookInitialized: Boolean

    fun isLightClientWanted(): Boolean
    fun getNightMode(): Int
    fun getStatsName(): String
}
