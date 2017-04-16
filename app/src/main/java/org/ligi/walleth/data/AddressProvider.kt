package org.ligi.walleth.data

data class AddressBookEntry(val name: String, val address: WallethAddress, val note: String? = null)

object AddressProvider {

    fun getNameForAddress(address: WallethAddress) = "foo"

    fun getAllAddresses() = listOf(
            AddressBookEntry("room77", WallethAddress("")),
            AddressBookEntry("faucet", WallethAddress("")),
            AddressBookEntry("BitSquare", WallethAddress(""))

    )
}