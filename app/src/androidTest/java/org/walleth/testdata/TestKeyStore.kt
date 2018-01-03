package org.walleth.testdata

import org.kethereum.model.Address
import org.walleth.data.keystore.WallethKeyStore
import java.util.*

private val random = Random()
fun randomAddress() = Address("0x"+(0 until 40).map { "0123456789abcdef"[Math.abs(random.nextInt()%16)] }.joinToString(""))

class TestKeyStore : WallethKeyStore {

    val addresses = mutableListOf<Address>()

    override fun hasKeyForForAddress(wallethAddress: Address) = addresses.contains(wallethAddress)

    override fun newAddress(password: String): Address {
        val newAddress = randomAddress()
        addresses.add(newAddress)
        return newAddress
    }

    override fun deleteKey(address: Address, password: String) {
        addresses.remove(address)
    }

    val import_result_address = Address("OxABCD43")

    override fun importJSONKey(json: String, importPassword: String, storePassword: String): Address {
        if (importPassword == "bad password") {
            throw(IllegalArgumentException("Bad Password"))
        }
        return import_result_address
    }

    override fun importECDSAKey(key: String, storePassword: String) = import_result_address

    override fun exportKey(address: Address, unlockPassword: String, exportPassword: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAddressByIndex(index: Int): Address {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAddressCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}