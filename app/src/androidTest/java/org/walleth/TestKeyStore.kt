package org.walleth

import org.ligi.walleth.data.WallethAddress
import org.ligi.walleth.data.keystore.WallethKeyStore

class TestKeyStore : WallethKeyStore {
    override fun getCurrentAddress() = WallethAddress("0xfdf1210fc262c73d0436236a0e07be419babbbc4")
    override fun importKey(json: String, importPassword: String, newPassword: String) = WallethAddress("OxABCD43")
    override fun exportCurrentKey(unlockPassword: String, exportPassword: String) = "export_key" + unlockPassword + exportPassword
}