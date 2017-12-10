package org.walleth.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.walleth.R
import org.walleth.data.networks.NetworkDefinition

class NetworkAdapter(val list: List<NetworkDefinition>,
                     private val onClickAction: (entry: NetworkDefinition) -> Unit,
                     private val onInfoClick: (entry: NetworkDefinition) -> Unit) : RecyclerView.Adapter<NetworkDefinitionViewHolder>() {

    override fun getItemCount() = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkDefinitionViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.item_network_definition, parent, false)
        return NetworkDefinitionViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: NetworkDefinitionViewHolder, position: Int) {
        holder.bind(list[position], onClickAction, onInfoClick)
    }

}