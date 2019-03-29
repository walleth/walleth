package org.walleth.data.transactions

data class TransactionState(var needsSigningConfirmation: Boolean = false,
                            var relayed: String = "",
                            var eventLog: String? = null,
                            var isPending: Boolean = true,
                            var error: String? = null)