package org.walleth.security

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

fun isDappNodeReachable() = try {
    val sockaddr = InetSocketAddress("172.33.1.9", 80)
    val sock = Socket()

    val timeoutMs = 2000 // 2 seconds

    sock.connect(sockaddr, timeoutMs)
    true
} catch (e: IOException) {
    false
}