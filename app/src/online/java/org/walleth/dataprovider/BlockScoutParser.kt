package org.walleth.dataprovider

import org.json.JSONArray

class ParseResult(val list: List<String>, val highestBlock: Long)

fun parseEtherscanTransactionList(jsonArray: JSONArray): ParseResult {
    var lastBlockNumber = 0L
    val list = (0 until jsonArray.length()).map {
        val transactionJson = jsonArray.getJSONObject(it)
        val blockNumber = transactionJson.getString("blockNumber").toLong()
        lastBlockNumber = Math.max(blockNumber, lastBlockNumber)
        transactionJson.getString("hash")
    }
    return ParseResult(list, lastBlockNumber)
}
