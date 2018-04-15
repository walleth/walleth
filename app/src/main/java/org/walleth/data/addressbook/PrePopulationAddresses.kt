package org.walleth.data.addressbook

import org.kethereum.model.Address


var ligi = AddressBookEntry(
        name = "LIGI",
        address = Address("0x381e247bef0ebc21b6611786c665dd5514dcc31f"),
        note = "Developer & Ideator - send some ETH if you like this project and want it to continue",
        isNotificationWanted = false,
        trezorDerivationPath = null
)

val michael = AddressBookEntry(
        name = "Michael Cook",
        address = Address("0xbE27686a93c54Af2f55f16e8dE9E6Dc5dccE915e"),
        note = "Icon designer - please tip him well if you want things to look nice",
        isNotificationWanted = false,
        trezorDerivationPath = null
)

val faucet = AddressBookEntry(
        name = "Faucet",
        address = Address("0x31b98d14007bdee637298086988a0bbd31184523"),
        note = "The source of some rinkeby ether",
        isNotificationWanted = false,
        trezorDerivationPath = null
)

val allPrePopulationAddresses = listOf(michael, ligi, faucet)