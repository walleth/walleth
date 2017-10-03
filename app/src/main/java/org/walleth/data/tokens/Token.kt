package org.walleth.data.tokens

import android.arch.persistence.room.Entity
import org.kethereum.model.Address
import org.walleth.data.networks.NetworkDefinition
import org.kethereum.model.ChainDefinition

fun Token.isETH() = address.hex == "0x0"

fun getEthTokenForChain(networkDefinition: NetworkDefinition) = Token("ETH", decimals = 18, address = Address("0x0"), chain = networkDefinition.chain)

@Entity(tableName = "tokens", primaryKeys = arrayOf("address", "chain"))
data class Token(
        val name: String,
        val decimals: Int,
        val address: Address,
        val chain: ChainDefinition
)