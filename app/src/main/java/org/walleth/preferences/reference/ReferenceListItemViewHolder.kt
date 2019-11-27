package org.walleth.preferences.reference

import android.app.Activity
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fiat_list_item.view.*
import org.threeten.bp.format.DateTimeFormatter
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.FiatInfo

class ReferenceListItemViewHolder(itemView: View,
                                  val activity: Activity,
                                  val settings: Settings,
                                  private val timeFormatter: DateTimeFormatter?) : RecyclerView.ViewHolder(itemView) {

    fun bind(fiatInfo: FiatInfo) {
        itemView.fiat_symbol.text = fiatInfo.symbol
        itemView.fiat_update_date.text = "updated: " + (fiatInfo.lastUpdated?.format(timeFormatter) ?: "?")

        itemView.fiat_exchange_rate.text = "rate: " + if (fiatInfo.exchangeRate == null) "?" else String.format("%.2f", fiatInfo.exchangeRate)

        itemView.setOnClickListener {
            settings.currentFiat = fiatInfo.symbol
            activity.finish()
        }
    }
}