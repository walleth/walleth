package org.walleth.data.keystore

import org.kethereum.model.Address
import org.walleth.data.Observeable

interface WallethKeyStore : Observeable {

    fun getCurrentAddress(): Address
    fun setCurrentAddress(address: Address)
    fun importJSONKey(json: String, importPassword: String, storePassword: String): Address?
    fun importECDSAKey(key: String, storePassword: String): Address?
    fun exportCurrentKey(unlockPassword: String, exportPassword: String): String

    fun newAddress(password: String): Address
    fun deleteKey(address: Address, password: String)
    fun hasKeyForForAddress(wallethAddress: Address): Boolean
}