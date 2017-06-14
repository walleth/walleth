package org.walleth.ui

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.walleth.R
import org.walleth.data.exchangerate.TokenProvider

class TokenListAdapter(val tokenProvider: TokenProvider, val activity: Activity) : RecyclerView.Adapter<TokenViewHolder>() {
    val tokenList = tokenProvider.getAllTokens()

    override fun getItemCount() = tokenList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = TokenViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.token_list_item, parent, false), activity, tokenProvider)

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        holder.bind(tokenList[position])
    }

}