package org.walleth.data.transactions

data class TransactionState(var needsSigningConfirmation: Boolean = false,
                            var source: TransactionSource = TransactionSource.WALLETH,
                            var relayedLightClient: Boolean = false,
                            var relayedEtherscan: Boolean = false,
                            var eventLog: String? = null,
                            var isPending: Boolean = true,
                            var gethSignProcessed: Boolean = false,
                            var error: String? = null)