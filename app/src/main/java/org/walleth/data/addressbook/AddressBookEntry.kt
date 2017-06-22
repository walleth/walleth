package org.walleth.data.addressbook

import org.kethereum.model.Address

data class AddressBookEntry(var name: String, var address: Address, var note: String? = null, var isNotificationWanted: Boolean = false)