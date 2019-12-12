package org.walleth.chains

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.walleth.R
import org.walleth.data.chaininfo.ChainInfo

class ChainAdapter(
        private val onAction: (entry: ChainInfo, action: ChainInfoViewAction) -> Unit) : RecyclerView.Adapter<ChainInfoViewHolder>() {

    var list: List<ChainInfo> = mutableListOf()
    var displayList: List<ChainInfo> = mutableListOf()

    override fun getItemCount() = displayList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChainInfoViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.item_network_definition, parent, false)
        return ChainInfoViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: ChainInfoViewHolder, position: Int) {
        holder.bind(displayList[position], onAction)
    }


    fun filter(newList: List<ChainInfo>, starredOnly: Boolean) {
        list = newList
        val newDisplayList = newList
                .asSequence()
                .filter { !starredOnly || it.starred }
                .filter { !it.softDeleted }
                .toList()

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = displayList.size

            override fun getNewListSize() = newDisplayList.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = displayList[oldItemPosition] == newDisplayList[newItemPosition]

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = displayList[oldItemPosition].chainId == newDisplayList[newItemPosition].chainId

        })

        diff.dispatchUpdatesTo(this)

        displayList = newDisplayList
    }

}