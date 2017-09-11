package org.walleth.ui

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.token_list_item.view.*
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.Token

class TokenViewHolder(itemView: View, val activity: Activity, val currentTokenProvider: CurrentTokenProvider) : RecyclerView.ViewHolder(itemView) {
    fun bind(tokenDescriptor: Token) {
        itemView.token_symbol.text = tokenDescriptor.name
        itemView.setOnClickListener {
            currentTokenProvider.currentToken = tokenDescriptor
            activity.finish()
        }
    }
}