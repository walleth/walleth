package org.walleth.accounts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.walleth.R
import org.walleth.data.addresses.AccountKeySpec

class AccountTypeAdapter(val list: List<AccountType>,
                         private val inSpec: AccountKeySpec) : RecyclerView.Adapter<AccountTypeViewHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int) = AccountTypeViewHolder(LayoutInflater.from(p0.context).inflate(R.layout.item_account_type, p0, false), inSpec)


    override fun getItemCount() = list.size

    override fun onBindViewHolder(viewHolder: AccountTypeViewHolder, p1: Int) {
        viewHolder.bind(list[p1])
    }
}