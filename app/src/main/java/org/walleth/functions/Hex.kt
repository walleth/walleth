package org.walleth.functions

/**
 *  chars for nibble
 */
private val CHARS = "0123456789abcdef"

/**
 *  Returns 2 char hex string for Byte
 */
internal fun Byte.toHexString() = toInt().let {
    CHARS[it.shr(4) and 0x0f].toString() + CHARS[it.and(0x0f)].toString()
}

fun fromHexToByteArray(hex: String) = ByteArray(hex.length / 2).apply {
    var i = 0
    while (i < hex.length) {
        this[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        i += 2
    }
}