package org.walleth.ui

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_network_definition.view.*
import org.walleth.data.networks.NetworkDefinition

class NetworkDefinitionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(networkDefinition: NetworkDefinition, onClickAction: (entry: NetworkDefinition) -> Unit) {

        itemView.network_title.text = networkDefinition.getNetworkName()

        itemView.setOnClickListener {
            onClickAction.invoke(networkDefinition)
        }

    }

}