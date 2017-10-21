package org.walleth.ui

import android.app.Activity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.walleth.R
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.Token


class TokenListAdapter(private val tokenProvider: CurrentTokenProvider,

                       private val activity: Activity) : RecyclerView.Adapter<TokenViewHolder>() {

    val tokenList = mutableListOf<Token>()

    override fun getItemCount() = tokenList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = TokenViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.token_list_item, parent, false), activity, tokenProvider)

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        holder.bind(tokenList[position])
    }

    fun updateTokenList(newTokenList: List<Token>) {
        val diffCallback = TokenDiffCallback(tokenList, newTokenList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        tokenList.clear()
        tokenList.addAll(newTokenList)
        diffResult.dispatchUpdatesTo(this)
    }

    class TokenDiffCallback(val old: List<Token>, val new: List<Token>) : DiffUtil.Callback() {
        override fun areItemsTheSame(p0: Int, p1: Int) = old[p0] == new[p1]

        override fun getOldListSize() = old.size

        override fun getNewListSize() = new.size

        override fun areContentsTheSame(p0: Int, p1: Int) = old[p0] == new[p1]

    }
}