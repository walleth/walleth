package org.ligi.walleth.data.addressbook

import org.ligi.walleth.data.WallethAddress

class FileBackedAddressBook : BaseAddressBook(), AddressBook {

    override var addresses: MutableMap<WallethAddress, AddressBookEntry> = mutableMapOf()

}