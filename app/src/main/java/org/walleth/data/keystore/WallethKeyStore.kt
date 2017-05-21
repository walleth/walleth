package org.walleth.data.keystore

import org.walleth.data.Observeable
import org.walleth.data.WallethAddress

interface WallethKeyStore : Observeable {

    fun getCurrentAddress(): WallethAddress
    fun setCurrentAddress(address: WallethAddress)
    fun importKey(json: String, importPassword: String, newPassword: String): WallethAddress?
    fun exportCurrentKey(unlockPassword: String, exportPassword: String): String

    fun newAddress(password: String): WallethAddress
    fun deleteKey(address: WallethAddress, password: String)
    fun hasKeyForForAddress(wallethAddress: WallethAddress): Boolean
}