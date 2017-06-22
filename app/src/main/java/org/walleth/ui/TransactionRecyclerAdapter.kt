package org.walleth.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import org.walleth.R
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.exchangerate.TokenProvider
import org.walleth.data.transactions.TransactionWithState

enum class TransactionAdapterDirection {
    INCOMMING, OUTGOING
}

class TransactionRecyclerAdapter(val transactionList: List<TransactionWithState>,
                                 val addressBook: AddressBook,
                                 val tokenProvider: TokenProvider,
                                 val direction: TransactionAdapterDirection) : RecyclerView.Adapter<TransactionViewHolder>() {

    override fun getItemCount() = transactionList.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) = holder.bind(transactionList[position], addressBook,tokenProvider)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.transaction_item, null)
        val layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        val margin = parent.context.resources.getDimension(R.dimen.rythm).toInt()
        layoutParams.setMargins(0, margin, 0, margin)
        itemView.layoutParams = layoutParams
        return TransactionViewHolder(itemView,direction)
    }

}