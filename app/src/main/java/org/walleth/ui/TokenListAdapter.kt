package org.walleth.ui

import android.app.Activity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.walleth.R
import org.walleth.data.AppDatabase

import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.Token


class TokenListAdapter(private val tokenProvider: CurrentTokenProvider,
                       private val activity: Activity,
                       private val database: AppDatabase) : RecyclerView.Adapter<TokenViewHolder>() {

    private var tokenList = listOf<Token>()

    var sortedList = listOf<Token>()

    override fun getItemCount() = sortedList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = TokenViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.token_list_item, parent, false), activity, tokenProvider, database)

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        holder.bind(sortedList[position])
    }

    fun updateTokenList(newTokenList: List<Token>, query: CharSequence, starredOny: Boolean) {
        tokenList = newTokenList
        filter(query, starredOny)
    }

    fun filter(search: CharSequence, starredOny: Boolean) {
        val newSortedList = tokenList.filter {
            !starredOny || it.starred
        }.filter {
            it.symbol.contains(search, true) || it.name.contains(search, true)
        }.sortedBy { it.order }

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = sortedList.size

            override fun getNewListSize() = newSortedList.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int)
                    = sortedList[oldItemPosition] == newSortedList[newItemPosition]

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int)
                    = sortedList[oldItemPosition].address == newSortedList[newItemPosition].address

        })

        diff.dispatchUpdatesTo(this)

        sortedList = newSortedList
    }

}