package org.walleth.util

import org.kethereum.model.ChainDefinition
import org.kethereum.model.ChainId
import org.walleth.data.networks.ALL_NETWORKS

fun ChainId.findChainDefinition() = ALL_NETWORKS.find { it.chain.id == this }?.chain ?: ChainDefinition(this, "?")