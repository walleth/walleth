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

fun ByteArray.toHexString(prefix: String = "0x") = prefix + this.map { "" + it.toHexString() }.joinToString("")
fun List<Byte>.toHexString(prefix: String = "0x") = toByteArray().toHexString(prefix)

fun fromHexToByteArray(hex: String): ByteArray {
    if (hex.length % 2 != 0)
        throw IllegalArgumentException("hex-string must have an even number of digits (nibbles)")

    val cleanInput = if (hex.startsWith("0x")) hex.substring(2) else hex

    return ByteArray(cleanInput.length / 2).apply {
        var i = 0
        while (i < cleanInput.length) {
            this[i / 2] = ((Character.digit(cleanInput[i], 16) shl 4) + Character.digit(cleanInput[i + 1], 16)).toByte()
            i += 2
        }
    }
}