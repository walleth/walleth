package org.ligi.walleth.data.config

interface Settings {
    var currentFiat: String
    fun getNightMode(): Int
}
