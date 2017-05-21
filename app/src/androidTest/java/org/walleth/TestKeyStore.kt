package org.walleth

import org.walleth.data.SimpleObserveable
import org.walleth.data.WallethAddress
import org.walleth.data.keystore.WallethKeyStore
import java.util.*

class TestKeyStore : SimpleObserveable(), WallethKeyStore {

    val addresses = mutableListOf<WallethAddress>()

    override fun hasKeyForForAddress(wallethAddress: WallethAddress) = addresses.contains(wallethAddress)

    override fun newAddress(password: String): WallethAddress {
        val newAddress = WallethAddress("0x" + UUID.randomUUID().toString())
        addresses.add(newAddress)
        return newAddress
    }

    override fun deleteKey(address: WallethAddress, password: String) {
        addresses.remove(address)
    }

    private var currentAddressVar = WallethAddress("0xfdf1210fc262c73d0436236a0e07be419babbbc4")

    val import_result_address = WallethAddress("OxABCD43")

    override fun setCurrentAddress(address: WallethAddress) {
        currentAddressVar = address
    }

    override fun getCurrentAddress() = currentAddressVar

    override fun importKey(json: String, importPassword: String, newPassword: String): WallethAddress {
        if (importPassword == "bad password") {
            throw(IllegalArgumentException("Bad Password"))
        }
        return import_result_address
    }

    override fun exportCurrentKey(unlockPassword: String, exportPassword: String) = "export_key" + unlockPassword + exportPassword
}