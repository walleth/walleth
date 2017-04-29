package org.ligi.walleth.activities

import android.app.Activity
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_address_book.view.*
import org.ligi.walleth.data.addressbook.AddressBookEntry

class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(addressBookEntry: AddressBookEntry, activity: Activity) {
        itemView.setOnClickListener {
            activity.setResult(Activity.RESULT_OK, Intent().apply { putExtra("HEX", addressBookEntry.address.hex) })
            activity.finish()
        }
        itemView.address_name.text = addressBookEntry.name
        itemView.address_note.text = addressBookEntry.note
        itemView.address_hash.text = addressBookEntry.address.hex
    }

}