package org.walleth.data.keystore

import org.kethereum.crypto.ECKeyPair
import org.kethereum.model.Address

interface WallethKeyStore {
    fun importKey(key: ECKeyPair, password: String): Address?
    fun deleteKey(address: Address): Boolean
    fun getKeyForAddress(address: Address, password: String): ECKeyPair?
    fun hasKeyForForAddress(wallethAddress: Address): Boolean
    fun getAddresses(): Set<Address>
}