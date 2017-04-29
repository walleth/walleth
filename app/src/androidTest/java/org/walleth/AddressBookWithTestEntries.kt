package org.walleth

import org.ligi.walleth.data.WallethAddress
import org.ligi.walleth.data.addressbook.AddressBook
import org.ligi.walleth.data.addressbook.AddressBookEntry
import org.ligi.walleth.data.addressbook.BaseAddressBook

class AddressBookWithTestEntries : BaseAddressBook(), AddressBook {

    companion object {
        val Room77 = WallethAddress("0xF00")
        val ShapeShift = WallethAddress("0xBA3")
        val ΞBay = WallethAddress("0xBA1")
        val Faundation = WallethAddress("0xBA2")
        val Faucet = WallethAddress("0xBA3")
        val Ligi = WallethAddress("0xBA4")
    }

    override var addresses: MutableMap<WallethAddress, AddressBookEntry> = mutableMapOf(
            Room77 to AddressBookEntry("Room77", Room77),
            ShapeShift to AddressBookEntry("ShapeShift", ShapeShift),
            ΞBay to AddressBookEntry("ΞBay", ΞBay),
            Faundation to AddressBookEntry("Foundation", Faundation),
            Faucet to AddressBookEntry("Faucet", Faucet),
            Ligi to AddressBookEntry("Ligi", Room77)
    )

}