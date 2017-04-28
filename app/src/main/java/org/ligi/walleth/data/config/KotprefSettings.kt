package org.ligi.walleth.data.config

import com.chibatching.kotpref.KotprefModel

object KotprefSettings : KotprefModel(), Settings {

    override var currentFiat by stringPref(default = "USD")

}
