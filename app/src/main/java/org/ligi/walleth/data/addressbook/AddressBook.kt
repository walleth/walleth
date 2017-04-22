package org.ligi.walleth.data.addressbook

import org.ligi.walleth.data.Observeable
import org.ligi.walleth.data.WallethAddress

interface AddressBook : Observeable {

    fun getEntryForName(address: WallethAddress): AddressBookEntry

    fun setEntry(entry: AddressBookEntry)

}