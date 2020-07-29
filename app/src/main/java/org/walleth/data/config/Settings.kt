package org.walleth.data.config

import android.content.SharedPreferences
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


    var filterFaucet: Boolean
    var filterFastFaucet: Boolean
    var filterTincubeth: Boolean
    var logRPCRequests: Boolean

    var sourcifyBaseURL: String

    var dappNodeVPNProfile: String
    var dappNodeAutostartVPN: Boolean
    var dappNodeMode: DappNodeMode

    var toolbarBackgroundColor: Int
    var toolbarForegroundColor: Int

    fun isLightClientWanted(): Boolean
    fun getNightMode(): Int

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)

    fun getGasPriceFor(chainId: BigInteger): BigInteger
    fun storeGasPriceFor(gasPrice: BigInteger, chainId: BigInteger)
    fun isScreenshotsDisabled(): Boolean
    fun isAdvancedFunctionsEnabled(): Boolean
}

enum class DappNodeMode {
    DONT_USE,
    USE_WHEN_POSSIBLE,
    ONLY_USE_DAPPNODE
}