package org.walleth.ui

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.walleth.R
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.Token

class TokenListAdapter(val tokenProvider: CurrentTokenProvider, val tokenList: List<Token>, val activity: Activity) : RecyclerView.Adapter<TokenViewHolder>() {

    override fun getItemCount() = tokenList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = TokenViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.token_list_item, parent, false), activity, tokenProvider)

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        holder.bind(tokenList[position])
    }

}