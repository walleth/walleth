package org.walleth.functions

import org.walleth.data.WallethAddress
import org.walleth.data.addressbook.AddressBook

fun WallethAddress.resolveNameFromAddressBook(addressBook: AddressBook)
        = addressBook.getEntryForName(this)?.name ?: hex
