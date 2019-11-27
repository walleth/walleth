package org.walleth.data.addresses

import org.kethereum.model.Address

var ligi = AddressBookEntry(
        name = "LIGI",
        address = Address("0x381e247bef0ebc21b6611786c665dd5514dcc31f"),
        note = "Developer & Ideator"
)

val michael = AddressBookEntry(
        name = "Michael Cook",
        address = Address("0xbE27686a93c54Af2f55f16e8dE9E6Dc5dccE915e"),
        note = "Icon designer"
)

val faucet = AddressBookEntry(
        name = "Rinkeby Faucet",
        address = Address("0x31b98d14007bdee637298086988a0bbd31184523"),
        note = "The source of some rinkeby ether"
)

val goerli_pusher = AddressBookEntry(
        name = "Goerli pusher",
        address = Address("0x03e0ffece04d779388b7a1d5c5102ac54bd479ee"),
        note = "Mints TST tokens for you"
)

val goerli_simple_faucet = AddressBookEntry(
        name = "Goerli simple faucet",
        address = Address("0x8ced5ad0d8da4ec211c17355ed3dbfec4cf0e5b9")
)


val goerli_social_faucet = AddressBookEntry(
        name = "Goerli social faucet",
        address = Address("0x8c1e1e5b47980d214965f3bd8ea34c413e120ae4")
)


val allPrePopulationAddresses = listOf(michael, ligi, faucet, goerli_pusher, goerli_simple_faucet, goerli_social_faucet)