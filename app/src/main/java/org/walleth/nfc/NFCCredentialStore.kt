package org.walleth.nfc

import android.content.Context
import im.status.keycard.applet.Pairing
import org.komputing.khex.extensions.toNoPrefixHexString
import java.io.File

class NFCCredentialStore(context: Context) {

    private val pairingStorePath = File(context.filesDir, "sc_pairings").apply {
        mkdirs()
    }

    fun putPairing(uid: ByteArray, pairing: Pairing) {
        getFileForUID(uid).writeText(pairing.toBase64())
    }

    private fun getFileForUID(uid: ByteArray) = File(pairingStorePath, uid.toNoPrefixHexString())

    fun hasPairing(uid: ByteArray) = getFileForUID(uid).exists()
    fun getPairing(uid: ByteArray) = Pairing(getFileForUID(uid).readText())
}
