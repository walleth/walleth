package org.walleth.ui

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.kethereum.keystore.api.KeyStore
import org.walleth.R
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.addressbook.faucet

class AddressAdapter(val keyStore: KeyStore,
                     val onClickAction: (entry: AddressBookEntry) -> Unit,
                     val appDatabase: AppDatabase) : RecyclerView.Adapter<AddressViewHolder>() {

    var list = listOf<AddressBookEntry>()
    var displayList = listOf<AddressBookEntry>()

    override fun getItemCount() = displayList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.item_address_book, parent, false)
        return AddressViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(displayList[position], keyStore, onClickAction, appDatabase)
    }

    fun filter(starredOnly: Boolean, writableOnly: Boolean) {
        val newDisplayList = list
                .asSequence()
                .filter { !starredOnly || it.starred  }
                .filter { !writableOnly || keyStore.hasKeyForForAddress(it.address) }
                .filter { !it.deleted }
                .filter { it != faucet }
                .sortedBy { it.name }
                .toList()

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = displayList.size

            override fun getNewListSize() = newDisplayList.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int)
                    = displayList[oldItemPosition] == newDisplayList[newItemPosition]

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int)
                    = displayList[oldItemPosition].address == newDisplayList[newItemPosition].address

        })

        diff.dispatchUpdatesTo(this)

        displayList = newDisplayList
    }

}