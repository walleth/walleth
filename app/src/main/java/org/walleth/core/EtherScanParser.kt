package org.walleth.core

import org.json.JSONArray
import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.walleth.data.transactions.TransactionSource
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.TransactionWithState
import org.walleth.khex.hexToByteArray
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
                        nonce = try {
                            BigInteger(transactionJson.getString("nonce"))
                        } catch (e:NumberFormatException) {
                            null
                        },
                        input = transactionJson.getString("input").hexToByteArray().toList(),
                        txHash = transactionJson.getString("hash"),
                        creationEpochSecond = timeStamp
                ),
                TransactionState(false,TransactionSource.ETHERSCAN)
        )
    }
}
