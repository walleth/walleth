package org.walleth.ui

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.fiat_list_item.view.*
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.FiatInfo

class FiatListItemViewHolder(itemView: View, val activity: Activity, val settings: Settings) : RecyclerView.ViewHolder(itemView) {
    fun bind(fiatInfo: FiatInfo) {
        itemView.fiat_symbol.text = fiatInfo.symbol
        itemView.fiat_update_date.text = "updated: " + (fiatInfo.lastUpdated?.toString() ?: "?")

        itemView.fiat_exchange_rate.text = "rate: " + if (fiatInfo.exchangeRate == null) "?" else String.format("%.2f", fiatInfo.exchangeRate)

        itemView.setOnClickListener {
            settings.currentFiat = fiatInfo.symbol
            activity.finish()
        }
    }
}