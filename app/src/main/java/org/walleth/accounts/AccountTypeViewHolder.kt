package org.walleth.accounts

import android.app.Activity
import androidx.recyclerview.widget.RecyclerView
import org.walleth.data.addresses.AccountKeySpec
import org.walleth.databinding.ItemAccountTypeBinding

class AccountTypeViewHolder(private val itemBinding: ItemAccountTypeBinding, private val inSpec: AccountKeySpec) : RecyclerView.ViewHolder(itemBinding.root) {
    fun bind(item: AccountType) {
        itemBinding.bitmapType.isEnabled = false
        itemBinding.bitmapType.setImageResource(item.drawable)
        itemBinding.accountTypeLabel.text = item.action
        itemBinding.accountTypeDescription.text = item.description
        itemBinding.root.setOnClickListener {
            item.callback.invoke(itemView.context as Activity, inSpec)
        }
    }
}