package org.walleth.data.transactions

data class TransactionJSON(val value: String,
                           val to: String,
                           var nonce: String,
                           var gasPrice: String,
                           var gas: String,
                           var input: String,
                           var hash: String,
                           var v: String,
                           var r: String,
                           var s: String)
