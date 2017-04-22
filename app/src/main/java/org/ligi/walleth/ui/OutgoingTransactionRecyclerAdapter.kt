package org.ligi.walleth.ui

import org.ligi.walleth.App
import org.ligi.walleth.data.TransactionProvider
import java.math.BigInteger.ZERO

class OutgoingTransactionRecyclerAdapter(val transactionProvider: TransactionProvider) :BaseTransactionRecyclerAdapter() {

    override val transactionList by lazy { transactionProvider.getTransactionsForAddress(App.currentAddress!!).filter { it.value<ZERO } }

}