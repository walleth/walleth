package org.walleth.data.addressbook

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.Okio
import java.io.File

class FileBackedAddressBook(val context: Context) : BaseAddressBook(), AddressBook {

    val workPackageListType = Types.newParameterizedType(List::class.java, AddressBookEntry::class.java)
    val adapter: JsonAdapter<List<AddressBookEntry>> = Moshi.Builder().build().adapter(workPackageListType)!!

    val file = File(context.filesDir, "addresses.json")

    init {
        if (file.exists()) {
            adapter.fromJson(Okio.buffer(Okio.source(file))).forEach {
                setEntry(it)
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