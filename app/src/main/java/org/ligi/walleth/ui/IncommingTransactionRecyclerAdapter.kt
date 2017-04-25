package org.ligi.walleth.ui

import org.ligi.walleth.data.TransactionProvider
import org.ligi.walleth.data.WallethAddress
import java.math.BigInteger.ZERO

class IncommingTransactionRecyclerAdapter(val transactionProvider: TransactionProvider, address: WallethAddress) : BaseTransactionRecyclerAdapter() {

    override val transactionList by lazy { transactionProvider.getTransactionsForAddress(address).filter { it.value >= ZERO } }

}