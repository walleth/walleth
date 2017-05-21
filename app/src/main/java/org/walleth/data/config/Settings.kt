package org.walleth.data.config

interface Settings {
    var currentFiat: String
    var startupWarningDone: Boolean

    fun getNightMode(): Int
    fun getStatsName(): String
}
