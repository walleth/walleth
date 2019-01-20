package org.walleth.kethereum.model

import org.kethereum.model.Address
import org.kethereum.model.ChainDefinition

data class AddressOnChain(val address: Address, val chain: ChainDefinition)