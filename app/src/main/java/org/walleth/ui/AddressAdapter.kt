package org.walleth.ui

import android.support.v7.util.SortedList
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.util.SortedListAdapterCallback
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.coroutines.experimental.launch
import org.walleth.R
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.keystore.WallethKeyStore

class AddressAdapter(val keyStore: WallethKeyStore,
                     val onClickAction: (entry: AddressBookEntry) -> Unit,
                     val saveUpdatedAddress: (entry:AddressBookEntry) -> Unit) : RecyclerView.Adapter<AddressViewHolder>() {

    private val list = mutableListOf<AddressBookEntry>()
    private val sortedList = SortedList<AddressBookEntry>(AddressBookEntry::class.java, AddressAdapter.AddressAdapterCallback(this))

    override fun getItemCount() = sortedList.size()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.item_address_book, parent, false)
        return AddressViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(sortedList[position], keyStore, onClickAction, this::updateAddressBookEntry)
    }

    fun updateAddressList(newTokenList: List<AddressBookEntry>, starredOny: Boolean, writableOnly: Boolean) {
        list.clear()
        list.addAll(newTokenList)

        filter(starredOny, writableOnly)
    }


    fun filter(starredOnly: Boolean, writableOnly: Boolean) {
        sortedList.beginBatchedUpdates()
        sortedList.clear()

        for (address in list) {
            if ((!writableOnly || keyStore.hasKeyForForAddress(address.address)) && (address.starred || !starredOnly)) {
                sortedList.add(address)
            }
        }

        sortedList.endBatchedUpdates()
    }


    fun updateAddressBookEntry(oldAddress: AddressBookEntry, updatedAddress: AddressBookEntry) {
        list.remove(oldAddress)
        list.add(updatedAddress)
        launch {
            saveUpdatedAddress(updatedAddress)
        }
    }

    class AddressAdapterCallback(adapter: AddressAdapter) : SortedListAdapterCallback<AddressBookEntry>(adapter) {
        override fun areContentsTheSame(oldItem: AddressBookEntry?, newItem: AddressBookEntry?) = oldItem?.address == newItem?.address

        override fun compare(o1: AddressBookEntry?, o2: AddressBookEntry?): Int {
            if (o1 == null) {
                return if (o2 == null) 0 else -1
            } else {
                if (o2 == null) return 1
            }

            return o1.address.hex.compareTo(o2.address.hex)
        }

        override fun areItemsTheSame(item1: AddressBookEntry?, item2: AddressBookEntry?) = item1?.address == item2?.address
    }

}