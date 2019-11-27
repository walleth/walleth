package org.walleth.accounts

import android.app.Activity
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_account_type.view.*
import org.walleth.data.addresses.AccountKeySpec

class AccountTypeViewHolder(itemView: View, val inSpec: AccountKeySpec) : RecyclerView.ViewHolder(itemView) {
    fun bind(item: AccountType) {
        itemView.bitmap_type.isEnabled = false
        itemView.bitmap_type.setImageResource(item.drawable)
        itemView.account_type_label.text = item.action
        itemView.account_type_description.text = item.description
        itemView.setOnClickListener {
            item.callback.invoke(itemView.context as Activity, inSpec)
        }
    }
}