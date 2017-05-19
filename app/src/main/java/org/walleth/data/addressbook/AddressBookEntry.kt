package org.walleth.data.addressbook

import org.walleth.data.WallethAddress

data class AddressBookEntry(var name: String, var address: WallethAddress, var note: String? = null, var isNotificationWanted: Boolean = false)