package org.walleth.ui

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.token_list_item.view.*
import org.walleth.data.exchangerate.TokenProvider
import org.walleth.data.tokens.TokenDescriptor

class TokenViewHolder(itemView: View, val activity: Activity, val settings: TokenProvider) : RecyclerView.ViewHolder(itemView) {
    fun bind(tokenDescriptor: TokenDescriptor) {
        itemView.token_symbol.text = tokenDescriptor.name
        itemView.setOnClickListener {
            settings.currentToken = tokenDescriptor
            activity.finish()
        }
    }
}