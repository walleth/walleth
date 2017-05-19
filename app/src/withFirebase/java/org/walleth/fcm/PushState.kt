package org.walleth.fcm

import com.chibatching.kotpref.KotprefModel
import java.util.*

object PushState : KotprefModel() {
    val uuid by stringPref(default = UUID.randomUUID().toString())
}