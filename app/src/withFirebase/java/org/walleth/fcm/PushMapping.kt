package org.walleth.fcm

data class PushMapping(val uid: String, val pushToken: String, val addresses: List<String>)