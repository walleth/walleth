package org.walleth.data.config

import android.content.SharedPreferences

interface Settings {
    var currentFiat: String
    var startupWarningDone: Boolean

    var chain: Long
    var accountAddress: String?

    var addressInitVersion: Int
    var tokensInitVersion: Int

    var currentGoVerbosity: Int

    var showOnlyStaredTokens: Boolean

    fun isLightClientWanted(): Boolean
    fun getNightMode(): Int
    fun getStatsName(): String

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
}
