package org.walleth.data.config

import android.content.SharedPreferences
import org.walleth.data.networks.NetworkDefinition
import java.math.BigInteger

interface Settings {
    var currentFiat: String
    var startupWarningDone: Boolean

    var chain: Long
    var accountAddress: String?

    var addressInitVersion: Int
    var tokensInitVersion: Int

    var currentGoVerbosity: Int

    var showOnlyStaredTokens: Boolean


    var filterAddressesStared: Boolean
    var filterAddressesKeyOnly: Boolean

    var toolbarBackgroundColor: Int
    var toolbarForegroundColor: Int

    fun isLightClientWanted(): Boolean
    fun getNightMode(): Int
    fun getStatsName(): String

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)

    fun getGasPriceFor(current: NetworkDefinition): BigInteger
    fun storeGasPriceFor(gasPrice: BigInteger, network: NetworkDefinition)
}
