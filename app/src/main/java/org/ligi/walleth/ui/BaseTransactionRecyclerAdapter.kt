package org.ligi.walleth.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import org.ligi.walleth.R
import org.ligi.walleth.data.Transaction
import org.ligi.walleth.data.TransactionProvider

abstract class BaseTransactionRecyclerAdapter() : RecyclerView.Adapter<TransactionViewHolder>() {


    abstract val transactionList: List<Transaction>

    override fun getItemCount() = transactionList.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) = holder.bind(transactionList[position])

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.transaction_item , null)
        val layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        val margin = parent.context.resources.getDimension(R.dimen.rythm).toInt()
        layoutParams.setMargins(0, margin, 0, margin)
        itemView.layoutParams = layoutParams
        return TransactionViewHolder(itemView)
    }

}