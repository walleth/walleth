package org.walleth.enhancedlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList

class EnhancedListViewHolder(view: View) : RecyclerView.ViewHolder(view)

class EnhancedListAdapter<T : ListItem>(
        @LayoutRes
        private val layout: Int,
        private val bind: (entry: T, view: View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var list: List<T> = mutableListOf()
    var displayList: List<T> = mutableListOf()

    override fun getItemCount() = displayList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnhancedListViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return EnhancedListViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = displayList[position]
        bind.invoke(item, holder.itemView)
    }


    suspend fun filter(newList: List<T>,
                       filter: suspend (t: T) -> Boolean,
                       onChange: suspend () -> Unit,
                       areEqual: (t1: T, t2: T) -> Boolean) {
        if (list.size != newList.size || !list.containsAll(newList)) {
            onChange.invoke()
        }
        list = newList
        val newDisplayList = newList
                .asFlow()
                .filter { filter(it) }
                .filter { !it.deleted }
                .toList()

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = displayList.size

            override fun getNewListSize() = newDisplayList.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = displayList[oldItemPosition] == newDisplayList[newItemPosition]

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = areEqual(displayList[oldItemPosition], newDisplayList[newItemPosition])

        })

        diff.dispatchUpdatesTo(this)

        displayList = newDisplayList
    }

}