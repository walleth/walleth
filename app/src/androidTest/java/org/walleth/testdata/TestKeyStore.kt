package org.walleth.testdata

import org.kethereum.crypto.toAddress
import org.kethereum.keystore.api.KeyStore
import org.kethereum.model.Address
import org.kethereum.model.ECKeyPair

class TestKeyStore : KeyStore {

    private val addresses = mutableMapOf<Address, ECKeyPair>()

    override fun addKey(key: ECKeyPair, password: String, light: Boolean): Address? {

        if (password == "bad password") {
            throw(IllegalArgumentException("Bad Password"))
        }
        val element = key.toAddress()
        addresses[element] = key
        return element
    }

    override fun deleteKey(address: Address): Boolean {
        addresses.remove(address)
        return true
    }

    override fun getKeyForAddress(address: Address, password: String) = addresses[address]

    override fun getAddresses() = addresses.keys

    override fun hasKeyForForAddress(address: Address) = addresses.contains(address)

}