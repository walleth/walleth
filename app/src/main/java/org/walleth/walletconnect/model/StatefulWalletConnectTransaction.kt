package org.walleth.walletconnect.model

data class StatefulWalletConnectTransaction(val tx: WalletConnectTransaction,
                                            val session: Session,
                                            val id: String)