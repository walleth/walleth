package org.walleth.testdata

import org.kethereum.model.Address
import org.walleth.data.SimpleObserveable
import org.walleth.data.keystore.WallethKeyStore
import java.util.*

val DEFAULT_TEST_ADDRESS = Address("0xfdf1210fc262c73d0436236a0e07be419babbbc4")

class TestKeyStore : SimpleObserveable(), WallethKeyStore {

    val addresses = mutableListOf<Address>()

    override fun hasKeyForForAddress(wallethAddress: Address) = addresses.contains(wallethAddress)

    override fun newAddress(password: String): Address {
        val newAddress = Address("0x" + UUID.randomUUID().toString())
        addresses.add(newAddress)
        return newAddress
    }

    override fun deleteKey(address: Address, password: String) {
        addresses.remove(address)
    }

    private var currentAddressVar = DEFAULT_TEST_ADDRESS

    val import_result_address = Address("OxABCD43")

    override fun setCurrentAddress(address: Address) {
        currentAddressVar = address
    }

    override fun getCurrentAddress() = currentAddressVar

    override fun importJSONKey(json: String, importPassword: String, storePassword: String): Address {
        if (importPassword == "bad password") {
            throw(IllegalArgumentException("Bad Password"))
        }
        return import_result_address
    }

    override fun exportCurrentKey(unlockPassword: String, exportPassword: String) = "export_key_json_" + unlockPassword + exportPassword

    override fun importECDSAKey(key: String, storePassword: String) = import_result_address

}