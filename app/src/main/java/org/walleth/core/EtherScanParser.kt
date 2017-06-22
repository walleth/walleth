package org.walleth.core

import org.json.JSONArray
import org.kethereum.functions.fromHexToByteArray
import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.walleth.data.transactions.TransactionSource
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.TransactionWithState
import java.math.BigInteger

fun parseEtherScanTransactions(jsonArray: JSONArray): List<TransactionWithState> {
    return (0..(jsonArray.length() - 1)).map {
        val transactionJson = jsonArray.getJSONObject(it)
        val value = BigInteger(transactionJson.getString("value"))
        val timeStamp = transactionJson.getString("timeStamp").toLong()
        TransactionWithState(
                Transaction(
                        value,
                        Address(transactionJson.getString("from")),
                        Address(transactionJson.getString("to")),
                        nonce = transactionJson.optLong("nonce"),
                        input = fromHexToByteArray(transactionJson.getString("input")).toList(),
                        txHash = transactionJson.getString("hash"),
                        creationEpochSecond = timeStamp
                ),
                TransactionState(false,TransactionSource.ETHERSCAN)
        )
    }
}
