package org.walleth.data.networks

import org.kethereum.ETHEREUM_NETWORK_RINKEBY
import org.kethereum.model.ChainDefinition

val RINKEBY_CHAIN_ID = 4L

class RinkebyNetworkDefinition : BaseNetworkDefinition() {

    override val etherscanPrefix = ETHEREUM_NETWORK_RINKEBY
    override val chain = ChainDefinition(RINKEBY_CHAIN_ID)

    override fun getNetworkName() = ETHEREUM_NETWORK_RINKEBY

    override val statsSuffix = ":Respect my authoritah!@stats.rinkeby.io"

    override val bootNodes = listOf("enode://a24ac7c5484ef4ed0c5eb2d36620ba4e4aa13b8c84684e1b4aab0cebea2ae45cb4d375b77eab56516d34bfbd3c1a833fc51296ff084b770b94fb9028c4d25ccf@52.169.42.101:30303?discport=30304")

    override val genesis = ""

    override val infoUrl = "https://www.rinkeby.io/"
}