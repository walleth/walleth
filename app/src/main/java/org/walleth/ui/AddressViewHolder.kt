package org.walleth.ui

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import kotlinx.android.synthetic.main.item_address_book.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kethereum.keystore.api.KeyStore
import org.walleth.R
import org.walleth.activities.EditAccountActivity
import org.walleth.activities.ExportKeyActivity
import org.walleth.activities.startAddressReceivingActivity
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry

class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(addressBookEntry: AddressBookEntry, keyStore: KeyStore,
             onClickAction: (entry: AddressBookEntry) -> Unit,
             appDatabase: AppDatabase) {

        val context = itemView.context

        itemView.setOnClickListener {
            onClickAction.invoke(addressBookEntry)
        }

        itemView.address_name.text = addressBookEntry.name

        val hasKeyForForAddress = keyStore.hasKeyForForAddress(addressBookEntry.address)
        when {
            hasKeyForForAddress -> R.drawable.ic_key
            addressBookEntry.trezorDerivationPath != null -> R.drawable.trezor_icon
            else -> R.drawable.ic_watch_only
        }.let { itemView.key_indicator.setImageResource(it) }

        if (hasKeyForForAddress) {
            itemView.key_indicator.setOnClickListener {
                context.startAddressReceivingActivity(addressBookEntry.address, ExportKeyActivity::class.java)
            }
        }

        if (addressBookEntry.note == null || addressBookEntry.note!!.isBlank()) {
            itemView.address_note.visibility = GONE
        } else {
            itemView.address_note.visibility = VISIBLE
            itemView.address_note.text = addressBookEntry.note
        }

        itemView.address_hash.text = addressBookEntry.address.hex

        itemView.edit_account.setOnClickListener {
            context.startAddressReceivingActivity(addressBookEntry.address, EditAccountActivity::class.java)
        }

        itemView.address_starred.setImageResource(
                if (addressBookEntry.starred) {
                    R.drawable.ic_star_24dp
                } else {
                    R.drawable.ic_star_border_24dp
                }
        )

        itemView.address_starred.setOnClickListener {
            GlobalScope.launch {
                val updatedEntry = addressBookEntry.copy(starred = !addressBookEntry.starred)
                appDatabase.addressBook.upsert(updatedEntry)
            }
        }
    }

}