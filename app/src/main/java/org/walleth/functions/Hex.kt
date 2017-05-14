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

