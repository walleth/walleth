package org.walleth.ui

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_network_definition.view.*
import org.walleth.data.networks.NetworkDefinition

class NetworkDefinitionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(networkDefinition: NetworkDefinition, onClickAction: (entry: NetworkDefinition) -> Unit,
             onInfoClickAction: (entry: NetworkDefinition, stats: Boolean) -> Unit) {

        itemView.network_title.text = networkDefinition.getNetworkName()

        itemView.setOnClickListener {
            onClickAction.invoke(networkDefinition)
        }

        itemView.info_indicator.setOnClickListener {
            onInfoClickAction.invoke(networkDefinition, false)
        }

        itemView.stats_indicator.setOnClickListener {
            onInfoClickAction.invoke(networkDefinition, true)
        }
    }

}