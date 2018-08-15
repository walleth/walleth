package org.walleth.walletconnect.model

data class WalletConnectTransaction(val from: String,
                                    val to: String,
                                    val nonce: String,
                                    val gasPrice: String,
                                    val gasLimit: String,
                                    val gas: String,
                                    val value: String,
                                    val data: String)