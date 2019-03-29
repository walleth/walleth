package org.walleth.dataprovider

import org.json.JSONArray
import org.kethereum.model.ChainDefinition

class ParseResult(val list: List<String>, val highestBlock: Long)

fun parseBlockScoutTransactionList(jsonArray: JSONArray, chain: ChainDefinition): ParseResult {
    var lastBlockNumber = 0L
    val list = (0 until jsonArray.length()).map {
        val transactionJson = jsonArray.getJSONObject(it)
        val blockNumber = transactionJson.getString("blockNumber").toLong()
        lastBlockNumber = Math.max(blockNumber, lastBlockNumber)
        transactionJson.getString("hash")
    }
    return ParseResult(list, lastBlockNumber)
}
