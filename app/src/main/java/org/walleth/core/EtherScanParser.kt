package org.walleth.core

import org.json.JSONArray
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.walleth.data.WallethAddress
import org.walleth.data.transactions.Transaction
import org.walleth.data.transactions.TransactionSource
import org.walleth.functions.fromHexToByteArray
import java.math.BigInteger

fun parseEtherScanTransactions(jsonArray: JSONArray): List<Transaction> {
    return (0..(jsonArray.length() - 1)).map {
        val transactionJson = jsonArray.getJSONObject(it)
        val value = BigInteger(transactionJson.getString("value"))
        val timeStamp = Instant.ofEpochSecond(transactionJson.getString("timeStamp").toLong())
        val ofInstant = LocalDateTime.ofInstant(timeStamp, ZoneOffset.systemDefault())
        Transaction(
                value,
                WallethAddress(transactionJson.getString("from")),
                WallethAddress(transactionJson.getString("to")),
                nonce = transactionJson.optLong("nonce"),
                ref = TransactionSource.ETHERSCAN,
                input = fromHexToByteArray(transactionJson.getString("input")).toList(),
                txHash = transactionJson.getString("hash"),
                localTime = ofInstant
        )
    }
}
