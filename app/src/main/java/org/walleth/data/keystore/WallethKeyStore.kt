package org.walleth.data.keystore

import org.kethereum.model.Address

interface WallethKeyStore {

    fun importJSONKey(json: String, importPassword: String, storePassword: String): Address?
    fun importECDSAKey(key: String, storePassword: String): Address?
    fun exportKey(address: Address, unlockPassword: String, exportPassword: String): String

    fun getAddressByIndex(index: Int): Address
    fun getAddressCount(): Int

    fun newAddress(password: String): Address
    fun deleteKey(address: Address, password: String)
    fun hasKeyForForAddress(wallethAddress: Address): Boolean
}