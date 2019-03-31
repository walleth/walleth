package org.walleth.data.config

import android.content.SharedPreferences
import org.walleth.data.networks.NetworkDefinition
import java.math.BigInteger

interface Settings {
    var currentFiat: String
    var onboardingDone: Boolean

    var chain: Long
    var accountAddress: String?

    var addressInitVersion: Int
    var tokensInitVersion: Int
    var dataVersion: Int

    var currentGoVerbosity: Int

    var showOnlyStaredTokens: Boolean
    var showOnlyTokensOnCurrentNetwork: Boolean


    var filterAddressesStared: Boolean
    var filterAddressesKeyOnly: Boolean

    var toolbarBackgroundColor: Int
    var toolbarForegroundColor: Int

    var showDebug: Boolean

    fun isLightClientWanted(): Boolean
    fun getNightMode(): Int
    fun getStatsName(): String

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)

    fun getGasPriceFor(current: NetworkDefinition): BigInteger
    fun storeGasPriceFor(gasPrice: BigInteger, network: NetworkDefinition)
    fun isScreenshotsDisabled(): Boolean
}
