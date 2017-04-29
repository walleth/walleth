package org.ligi.walleth.data.addressbook

import org.ligi.walleth.data.SimpleObserveable
import org.ligi.walleth.data.WallethAddress

abstract class BaseAddressBook : SimpleObserveable(), AddressBook {

    protected open var addresses: MutableMap<WallethAddress, AddressBookEntry> = mutableMapOf()

    override fun getEntryForName(address: WallethAddress) = addresses[address] ?: AddressBookEntry("unknown", address)

    override fun getAllEntries() : List<AddressBookEntry> {
        if (addresses.isEmpty()) {
            setEntry(AddressBookEntry(
                    "Michael Cook",
                    WallethAddress("0xbE27686a93c54Af2f55f16e8dE9E6Dc5dccE915e"),
                    "Icon designer - please tip him well if you want things to look nice"
            ))

            setEntry(AddressBookEntry(
                    "LIGI",
                    WallethAddress("0xfdf1210fc262c73d0436236a0e07be419babbbc4"),
                    "Developer & Ideator - send some ETH if you like this project and want it to continue"
            ))

        }

        return addresses.values.toList()
    }

    override fun setEntry(entry: AddressBookEntry) {
        addresses[entry.address] = entry
        promoteChange()
    }

}