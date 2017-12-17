package org.walleth.data.networks

import org.kethereum.ETHEREUM_NETWORK_MAIN
import org.kethereum.model.ChainDefinition

class MainNetNetworkDefinition : BaseNetworkDefinition() {

    override val etherscanPrefix = ""
    override val chain = ChainDefinition(1L)

    override fun getNetworkName() = ETHEREUM_NETWORK_MAIN

    override val statsSuffix = ""

    override val bootNodes = emptyList<String>()

    override val genesis = ""

    override val infoUrl = "https://ethstats.net/"
}