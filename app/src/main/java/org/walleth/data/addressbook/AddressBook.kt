package org.walleth.data.addressbook

import org.walleth.data.Observeable
import org.walleth.data.WallethAddress

interface AddressBook : Observeable {

    fun getEntryForName(address: WallethAddress): AddressBookEntry

    fun getAllEntries(): List<AddressBookEntry>

    fun setEntry(entry: AddressBookEntry)

}