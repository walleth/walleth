package org.walleth.testdata

import org.walleth.data.WallethAddress
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.addressbook.BaseAddressBook

class AddressBookWithTestEntries : BaseAddressBook(), AddressBook {

    init {
        reset()
    }

    companion object {
        val Room77 = WallethAddress("0x19205A3A3b2A69De6Dbf7f01ED13B2108B2c43e8")
        val ShapeShift = WallethAddress("0x79205A3A3b2A69De6Dbf7f01ED13B2108B2c43e2")
        val ΞBay = WallethAddress("0x29205A3A3b2A69De6Dbf7f01ED13B2108B2c43e9")
        val Faundation = WallethAddress("0x89205A3A3b2A69De6Dbf7f01ED13B2108B2c43e7")
        val Faucet = WallethAddress("0x31b98d14007bdee637298086988a0bbd31184523")
        val Ligi = WallethAddress("0xfdf1210fc262c73d0436236a0e07be419babbbc4")
    }

    fun reset() {
        addresses.clear()
        addresses.putAll(listOf(
                Room77 to AddressBookEntry("Room77", Room77),
                ShapeShift to AddressBookEntry("ShapeShift", ShapeShift),
                ΞBay to AddressBookEntry("ΞBay", ΞBay),
                Faundation to AddressBookEntry("Foundation", Faundation),
                Faucet to AddressBookEntry("Faucet", Faucet),
                Ligi to AddressBookEntry("Ligi", Room77)
        ))
    }
}