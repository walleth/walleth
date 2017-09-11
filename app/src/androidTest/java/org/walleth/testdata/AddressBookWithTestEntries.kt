package org.walleth.testdata

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.kethereum.model.Address
import org.walleth.data.addressbook.AddressBookDAO
import org.walleth.data.addressbook.AddressBookEntry

val Room77 = Address("0x19205A3A3b2A69De6Dbf7f01ED13B2108B2c43e8")
val ShapeShift = Address("0x79205A3A3b2A69De6Dbf7f01ED13B2108B2c43e5")
val ΞBay = Address("0x29205A3A3b2A69De6Dbf7f01ED13B2108B2c43e9")
val Faundation = Address("0x89205A3A3b2A69De6Dbf7f01ED13B2108B2c43e7")
val Faucet = Address("0x31b98d14007bdee637298086988a0bbd31184523")
val Ligi = Address("0xfdf1210fc262c73d0436236a0e07be419babbbc4")

fun AddressBookDAO.addTestAddresses() {
    async(UI) {
        async(CommonPool) {
            upsert(listOf(
                    AddressBookEntry(Room77, "Room77"),
                    AddressBookEntry(ShapeShift, "ShapeShift"),
                    AddressBookEntry(ΞBay, "ΞBay"),
                    AddressBookEntry(Faundation, "Foundation"),
                    AddressBookEntry(Faucet, "Faucet"),
                    AddressBookEntry(Ligi, "Ligi")
            ))
        }.await()
    }
}
