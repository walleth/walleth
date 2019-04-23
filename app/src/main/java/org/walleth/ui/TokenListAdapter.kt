package org.walleth.ui

import android.app.Activity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.walleth.R
import org.walleth.data.tokens.Token


class TokenListAdapter(private val activity: Activity) : RecyclerView.Adapter<TokenViewHolder>() {

    private var tokenList = listOf<Token>()

    var sortedList = listOf<Token>()

    override fun getItemCount() = sortedList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TokenViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.token_list_item, parent, false), activity)

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        holder.bind(sortedList[position])
    }

    fun updateTokenList(newTokenList: List<Token>) {
        tokenList = newTokenList
    }

    fun filter(search: CharSequence, starredOny: Boolean, withChain: Long?) {
        val newSortedList = tokenList.filter {
            !starredOny || it.starred
        }.filter {
            it.symbol.contains(search, true) || it.name.contains(search, true)
        }.filter {
            (withChain == null) || it.chain == withChain
        }.sortedByDescending { it.order }

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = sortedList.size

            override fun getNewListSize() = newSortedList.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = sortedList[oldItemPosition] == newSortedList[newItemPosition]

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = sortedList[oldItemPosition].address == newSortedList[newItemPosition].address

        })

        diff.dispatchUpdatesTo(this)

        sortedList = newSortedList
    }

}