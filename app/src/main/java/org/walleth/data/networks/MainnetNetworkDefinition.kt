package org.walleth.data.networks

import org.kethereum.ETHEREUM_NETWORK_MAIN
import org.kethereum.model.ChainDefinition

class MainnetNetworkDefinition : BaseNetworkDefinition() {

    override val etherscan_prefix = ""
    override val chain = ChainDefinition(1L)

    override fun getNetworkName() = ETHEREUM_NETWORK_MAIN

    override val statsSuffix = ""

    override val bootNodes = emptyList<String>()

    override val genesis = ""

    override val statsUrl = "https://ethstats.net/"

    override val infoUrl = "https://ethereum.org/"
}