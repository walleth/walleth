package org.walleth.ui

import android.app.Activity
import android.support.v7.util.SortedList
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.util.SortedListAdapterCallback
import android.view.LayoutInflater
import android.view.ViewGroup
import org.walleth.R
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.Token


class TokenListAdapter(private val tokenProvider: CurrentTokenProvider,

                       private val activity: Activity) : RecyclerView.Adapter<TokenViewHolder>() {

    val tokenList = mutableListOf<Token>()

    val sortedList = SortedList<Token>(Token::class.java, TokenListAdapterCallback(this))

    override fun getItemCount() = sortedList.size()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = TokenViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.token_list_item, parent, false), activity, tokenProvider)

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        holder.bind(sortedList[position])
    }

    fun updateTokenList(newTokenList: List<Token>, query: CharSequence) {
        tokenList.clear()
        tokenList.addAll(newTokenList)

        filter(query)
    }

    fun filter(search: CharSequence) {
        sortedList.beginBatchedUpdates()
        sortedList.clear()

        if (search.isEmpty()) {
            sortedList.addAll(tokenList)
        } else {
            for (token in tokenList) {
                if (token.symbol.contains(search, true) || token.name.contains(search, true)) {
                    sortedList.add(token)
                }
            }
        }

        sortedList.endBatchedUpdates()
    }

    class TokenListAdapterCallback(adapter: TokenListAdapter) : SortedListAdapterCallback<Token>(adapter) {
        override fun areContentsTheSame(oldItem: Token?, newItem: Token?) = oldItem?.address == newItem?.address

        override fun compare(o1: Token?, o2: Token?): Int {
            if (o1 == null) {
                return if (o2 == null) 0 else -1
            } else {
                if (o2 == null) return 1
            }

            return o1.symbol.compareTo(o2.symbol)
        }

        override fun areItemsTheSame(item1: Token?, item2: Token?) = item1?.address == item2?.address
    }
}