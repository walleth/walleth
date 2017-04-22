package org.ligi.walleth.data.addressbook

import org.ligi.walleth.data.SimpleObserveable
import org.ligi.walleth.data.WallethAddress

class FileBackedAddressBook : SimpleObserveable(), AddressBook {

    private var addresses: MutableMap<WallethAddress, AddressBookEntry> = mutableMapOf()

    override fun getEntryForName(address: WallethAddress) = addresses[address] ?: AddressBookEntry("unknown", address)

    override fun setEntry(entry: AddressBookEntry) {
        addresses[entry.address] = entry
        promoteChange()
    }

}