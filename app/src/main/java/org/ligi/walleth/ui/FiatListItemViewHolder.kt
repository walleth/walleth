package org.ligi.walleth.ui

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.fiat_list_item.view.*

import org.ligi.walleth.data.exchangerate.FiatInfo

class FiatListItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(fiatInfo: FiatInfo) {
        itemView.fiat_symbol.text = fiatInfo.symbol
        itemView.fiat_update_date.text = fiatInfo.lastUpdated.toString()
        itemView.fiat_exchange_rate.text = String.format("%.2f", fiatInfo.exchangeRate)
    }
}