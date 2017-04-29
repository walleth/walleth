package org.ligi.walleth.activities

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.ligi.walleth.R
import org.ligi.walleth.data.addressbook.AddressBookEntry

class AddressAdapter(val list: List<AddressBookEntry>, val activity: Activity) : RecyclerView.Adapter<AddressViewHolder>() {

    override fun getItemCount() = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.item_address_book, parent, false)
        return AddressViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(list[position], activity)
    }

}