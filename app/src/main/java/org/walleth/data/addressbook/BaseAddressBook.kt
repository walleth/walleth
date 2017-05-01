package org.walleth.data.addressbook

import org.walleth.data.SimpleObserveable
import org.walleth.data.WallethAddress

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

            setEntry(AddressBookEntry(
                    "Faucet",
                    WallethAddress("0x31b98d14007bdee637298086988a0bbd31184523"),
                    "The source of some rinkeby ether"
            ))


        }

        return addresses.values.toList()
    }

    override fun setEntry(entry: AddressBookEntry) {
        addresses[entry.address] = entry
        promoteChange()
    }

}