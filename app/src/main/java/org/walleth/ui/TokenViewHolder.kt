package org.walleth.ui

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.token_list_item.view.*
import kotlinx.coroutines.experimental.launch
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.Token
import org.walleth.data.tokens.isETH

class TokenViewHolder(itemView: View, val activity: Activity, private val currentTokenProvider: CurrentTokenProvider,
                      val appDatabase: AppDatabase) : RecyclerView.ViewHolder(itemView) {
    fun bind(tokenDescriptor: Token) {
        itemView.token_symbol.text = tokenDescriptor.symbol
        itemView.token_name.text = tokenDescriptor.name
        itemView.token_decimals.text = activity.getString(R.string.decimals_in_list, tokenDescriptor.decimals.toString())
        itemView.token_starred.isChecked = tokenDescriptor.starred
        if (!tokenDescriptor.isETH()) {
            itemView.token_address.text = tokenDescriptor.address.hex
            itemView.token_address.visibility = View.VISIBLE
        } else {
            itemView.token_address.visibility = View.GONE
        }

        itemView.setOnClickListener {
            currentTokenProvider.currentToken = tokenDescriptor
            activity.finish()
        }

        itemView.token_starred.setOnClickListener {
            launch {
                appDatabase.tokens.upsert(tokenDescriptor.copy(starred = !tokenDescriptor.starred))
            }
        }
    }
}