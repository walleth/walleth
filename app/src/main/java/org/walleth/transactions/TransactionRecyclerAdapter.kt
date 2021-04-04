package org.walleth.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.transactions.TransactionEntity

enum class TransactionAdapterDirection {
    INCOMING, OUTGOING
}

class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {

    override fun areItemsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
        return oldItem.hash == newItem.hash
    }

    override fun areContentsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
        return oldItem == newItem
    }
}


class TransactionRecyclerAdapter(val appDatabase: AppDatabase,
                                 private val direction: TransactionAdapterDirection,
                                 val chainInfoProvider: ChainInfoProvider,
                                 private val exchangeRateProvider: ExchangeRateProvider,
                                 val settings: Settings
) : PagingDataAdapter<TransactionEntity, TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) = holder.bind(getItem(position), appDatabase)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(itemView, direction, chainInfoProvider, exchangeRateProvider, settings)
    }

}