package org.walleth.preferences.reference

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.threeten.bp.format.DateTimeFormatter
import org.walleth.R
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider

class ReferenceListAdapter(exchangeRateProvider: ExchangeRateProvider, val activity: Activity, val settings: Settings) : RecyclerView.Adapter<ReferenceListItemViewHolder>() {

    private val availableFiatInfoList = exchangeRateProvider.getAvailableFiatInfoMap().values.toList()

    override fun getItemCount() = availableFiatInfoList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = ReferenceListItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.fiat_list_item, parent, false), activity, settings, DateTimeFormatter.ofPattern("H:mm:ss"))


    override fun onBindViewHolder(holder: ReferenceListItemViewHolder, position: Int) {
        holder.bind(availableFiatInfoList[position])
    }

}