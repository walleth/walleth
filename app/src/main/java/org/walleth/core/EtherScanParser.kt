package org.walleth.core

import org.json.JSONArray
import org.kethereum.model.Address
import org.kethereum.model.ChainDefinition
import org.kethereum.model.createTransactionWithDefaults
import org.walleth.data.transactions.TransactionEntity
import org.walleth.data.transactions.TransactionSource
import org.walleth.data.transactions.TransactionState

import org.walleth.khex.hexToByteArray
import java.math.BigInteger

fun parseEtherScanTransactions(jsonArray: JSONArray, chain: ChainDefinition): List<TransactionEntity> {
    return (0 until jsonArray.length()).map {
        val transactionJson = jsonArray.getJSONObject(it)
        val value = BigInteger(transactionJson.getString("value"))
        val timeStamp = transactionJson.getString("timeStamp").toLong()
        TransactionEntity(
                transactionJson.getString("hash"),
                createTransactionWithDefaults(
                        chain = chain,
                        value = value,
                        from = Address(transactionJson.getString("from")),
                        to = Address(transactionJson.getString("to")),
                        nonce = try {
                            BigInteger(transactionJson.getString("nonce"))
                        } catch (e: NumberFormatException) {
                            null
                        },
                        input = transactionJson.getString("input").hexToByteArray().toList(),
                        txHash = transactionJson.getString("hash"),
                        creationEpochSecond = timeStamp
                ),
                signatureData = null,
                transactionState = TransactionState(false, isPending = false, source =  TransactionSource.ETHERSCAN)
        )
    }
}
