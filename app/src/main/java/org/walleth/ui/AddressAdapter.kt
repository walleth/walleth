package org.walleth.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.walleth.R
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.keystore.WallethKeyStore

class AddressAdapter(val list: List<AddressBookEntry>,
                     val keyStore: WallethKeyStore,
                     val onClickAction: (entry: AddressBookEntry) -> Unit) : RecyclerView.Adapter<AddressViewHolder>() {

    override fun getItemCount() = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.item_address_book, parent, false)
        return AddressViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(list[position], keyStore, onClickAction)
    }

}