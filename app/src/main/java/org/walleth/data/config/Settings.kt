package org.walleth.data.config

interface Settings {
    var currentFiat: String
    var startupWarningDone: Boolean

    var chain: Long
    var accountAddress: String?

    fun getNightMode(): Int
    fun getStatsName(): String
}
