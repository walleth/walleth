package org.walleth.data.keystore

import org.walleth.data.WallethAddress

interface WallethKeyStore {
    fun getCurrentAddress(): WallethAddress
    fun importKey(json: String, importPassword: String, newPassword: String): WallethAddress?
    fun exportCurrentKey(unlockPassword: String, exportPassword : String): String
}