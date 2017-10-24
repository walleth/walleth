package org.walleth.data.blockexplorer

import org.kethereum.model.Address

class EtherscanBlockExplorer(private val prefix: String) {

    val baseAPIURL by lazy { "https://" + (if (prefix.isBlank()) "api" else prefix) + ".etherscan.io/" }
    private val baseURL by lazy { "https://" + (if (prefix.isBlank()) "" else (prefix + ".")) + "etherscan.io/" }

    fun getURLforAddress(address: Address) = "$baseURL/address/${address.hex}"
    fun getURLforTransaction(transactionHash: String) = "$baseURL/tx/$transactionHash"
    fun getURLforBlock(blockNum: Long) = "$baseURL/block/$blockNum"

}