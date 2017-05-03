package org.walleth.ui

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_address_book.view.*
import org.walleth.data.addressbook.AddressBookEntry

class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(addressBookEntry: AddressBookEntry, onClickAction: (entry: AddressBookEntry) -> Unit) {
        itemView.setOnClickListener {
            onClickAction.invoke(addressBookEntry)
        }
        itemView.address_name.text = addressBookEntry.name
        itemView.address_note.text = addressBookEntry.note
        itemView.address_hash.text = addressBookEntry.address.hex
    }

}