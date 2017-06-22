package org.walleth.data.addressbook

import org.kethereum.model.Address
import org.walleth.data.SimpleObserveable

abstract class BaseAddressBook : SimpleObserveable(), AddressBook {

    protected open var addresses: MutableMap<Address, AddressBookEntry> = mutableMapOf()

    override fun getEntryForName(address: Address) = addresses[address]

    override fun getAllEntries(): List<AddressBookEntry> {
        if (addresses.size < 2) {
            setEntrySilent(AddressBookEntry(
                    "Michael Cook",
                    Address("0xbE27686a93c54Af2f55f16e8dE9E6Dc5dccE915e"),
                    "Icon designer - please tip him well if you want things to look nice"
            ))

            setEntrySilent(AddressBookEntry(
                    "LIGI",
                    Address("0xfdf1210fc262c73d0436236a0e07be419babbbc4"),
                    "Developer & Ideator - send some ETH if you like this project and want it to continue"
            ))

            setEntrySilent(AddressBookEntry(
                    "Faucet",
                    Address("0x31b98d14007bdee637298086988a0bbd31184523"),
                    "The source of some rinkeby ether"
            ))


        }

        return addresses.values.toList()
    }

    private fun setEntrySilent(entry: AddressBookEntry) {
        addresses[entry.address] = entry
    }

    override fun setEntry(entry: AddressBookEntry) {
        addresses[entry.address] = entry
        promoteChange()
    }

}