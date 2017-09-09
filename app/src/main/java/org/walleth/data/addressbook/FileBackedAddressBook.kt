package org.walleth.data.addressbook

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.Okio
import org.kethereum.model.Address
import java.io.File

class FileBackedAddressBook(val context: Context) : BaseAddressBook(), AddressBook {

    val addressBookType = Types.newParameterizedType(List::class.java, AddressBookEntry::class.java)!!
    val adapter: JsonAdapter<List<AddressBookEntry>> = Moshi.Builder().build().adapter(addressBookType)!!

    val file = File(context.filesDir, "addresses.json")

    init {
        if (file.exists()) {
            Okio.buffer(Okio.source(file)).use { buffer ->
                adapter.fromJson(buffer)?.forEach {
                    // we need to recreate the address to get the hex from cleanHex again
                    super.setEntry(it.copy(address = Address(it.address.cleanHex)))
                }
            }
        }
    }

    override fun setEntry(entry: AddressBookEntry) {
        super.setEntry(entry)
        Okio.buffer(Okio.sink(file)).use {
            it.writeUtf8(adapter.toJson(addresses.values.toList()))
        }
    }

}