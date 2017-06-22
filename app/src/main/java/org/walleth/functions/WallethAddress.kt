package org.walleth.functions

import org.kethereum.model.Address
import org.walleth.data.addressbook.AddressBook

fun Address.resolveNameFromAddressBook(addressBook: AddressBook)
        = addressBook.getEntryForName(this)?.name ?: hex
