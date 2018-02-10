package org.walleth.data.keystore

import org.kethereum.model.Address

class NoGethWallethKeyStore : WallethKeyStore {
    override fun importJSONKey(json: String, importPassword: String, storePassword: String): Address? = null

    override fun importECDSAKey(key: String, storePassword: String): Address? = null

    override fun exportKey(address: Address, unlockPassword: String, exportPassword: String): String = ""

    override fun getAddressByIndex(index: Int): Address = Address("")

    override fun getAddressCount(): Int = 0

    override fun newAddress(password: String): Address = Address("")

    override fun deleteKey(address: Address, password: String) = Unit

    override fun hasKeyForForAddress(wallethAddress: Address): Boolean = false
}
