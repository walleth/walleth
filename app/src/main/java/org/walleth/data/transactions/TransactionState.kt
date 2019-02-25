package org.walleth.data.transactions

data class TransactionState(var needsSigningConfirmation: Boolean = false,
                            var source: TransactionSource = TransactionSource.WALLETH,
                            var relayed: String = "",
                            var eventLog: String? = null,
                            var isPending: Boolean = true,
                            var error: String? = null)