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

val ChainIdToDappNodeRPC = mapOf(
        1 to listOf("http://geth.dappnode:8545", "http://nethermind.public.dappnode:8545", "http://openethereum.dappnode:8545"),
        3 to listOf("http://ropsten.dappnode:8545"),
        4 to listOf("http://rinkeby.dappnode:8545"),
        5 to listOf("http://goerli-geth.dappnode:8545"),
        42 to listOf("http://kovan.dappnode:8545")
)