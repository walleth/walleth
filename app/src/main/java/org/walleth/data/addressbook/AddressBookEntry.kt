package org.walleth.data.addressbook

import org.walleth.data.WallethAddress

data class AddressBookEntry(var name: String, val address: WallethAddress, var note: String? = null)