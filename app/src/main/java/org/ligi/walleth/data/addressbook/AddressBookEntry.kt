package org.ligi.walleth.data.addressbook

import org.ligi.walleth.data.WallethAddress

data class AddressBookEntry(var name: String, val address: WallethAddress, var note: String? = null)