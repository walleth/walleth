package org.walleth.data.addressbook

import org.walleth.data.WallethAddress

class FileBackedAddressBook : BaseAddressBook(), AddressBook {

    override var addresses: MutableMap<WallethAddress, AddressBookEntry> = mutableMapOf()

}