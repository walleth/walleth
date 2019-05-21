package org.walleth.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.walleth.R
import org.walleth.data.chaininfo.ChainInfo

class NetworkAdapter(val list: List<ChainInfo>,
                     private val onClickAction: (entry: ChainInfo) -> Unit,
                     private val onInfoClick: (entry: ChainInfo) -> Unit) : RecyclerView.Adapter<NetworkDefinitionViewHolder>() {

    override fun getItemCount() = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkDefinitionViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.item_network_definition, parent, false)
        return NetworkDefinitionViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: NetworkDefinitionViewHolder, position: Int) {
        holder.bind(list[position], onClickAction, onInfoClick)
    }

}