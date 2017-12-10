package org.walleth.ui

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_network_definition.view.*
import org.walleth.data.networks.NetworkDefinition

class NetworkDefinitionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(networkDefinition: NetworkDefinition, onClickAction: (entry: NetworkDefinition) -> Unit,
             onInfoClick: (entry: NetworkDefinition) -> Unit) {

        itemView.network_title.text = networkDefinition.getNetworkName()
        itemView.info_indicator.visibility = if (networkDefinition.infoUrl.isNotEmpty()) View.VISIBLE else View.INVISIBLE

        itemView.setOnClickListener {
            onClickAction.invoke(networkDefinition)
        }

        itemView.info_indicator.setOnClickListener {
            onInfoClick.invoke(networkDefinition)
        }

    }

}