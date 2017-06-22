package org.walleth.data.addressbook

import org.kethereum.model.Address
import org.walleth.data.Observeable

interface AddressBook : Observeable {

    fun getEntryForName(address: Address): AddressBookEntry?

    fun getAllEntries(): List<AddressBookEntry>

    fun setEntry(entry: AddressBookEntry)

}