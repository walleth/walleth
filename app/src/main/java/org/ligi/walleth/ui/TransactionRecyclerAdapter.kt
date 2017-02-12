package org.ligi.ewallet.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import org.ligi.walleth.R
import org.ligi.walleth.data.Transaction
import org.threeten.bp.LocalDateTime

class TransactionRecyclerAdapter : RecyclerView.Adapter<TransactionViewHolder>() {

    val now = LocalDateTime.now()

    val transactionList = arrayOf(

            Transaction(2000000000000000000, "Faucet ( 0x687422e.. )", now.minusHours(3)),
            Transaction(2000000000000000000, "Faucet (0x687422e..)", now.minusHours(5)),
            Transaction(-1000000000000000000, "BitSquare Trade (0xe242ba5..)", now.minusHours(6)),
            Transaction(2000000000000000000, "Faucet ( 0x687422e.. )", now.minusHours(6)),
            Transaction(-1000000000000000000, "Room77(0xadb2422e..)", now.minusHours(7)),
            Transaction(2000000000000000000, "Faucet (0x687422e..)", now.minusHours(10))

    )


    override fun getItemCount() = transactionList.size

    override fun getItemViewType(position: Int) = if (transactionList[position].value >= 0) 0 else 1

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactionList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(if (viewType == 0) R.layout.transaction_item else R.layout.transaction_item_send, null)
        val layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        val margin = parent.context.resources.getDimension(R.dimen.rythm).toInt()
        layoutParams.setMargins(0, margin, 0, margin)
        itemView.layoutParams = layoutParams
        return TransactionViewHolder(itemView)
    }

}