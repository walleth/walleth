package org.walleth.data.config

interface Settings {
    var currentFiat: String
    fun getNightMode(): Int
    fun getStatsName(): String
}
