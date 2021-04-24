package org.walleth.infrastructure

import org.ligi.trulesk.AppReplacingRunnerBase
import org.walleth.BuildConfig

class AppReplacingRunner : AppReplacingRunnerBase() {

    init {
        if (BuildConfig.FLAVOR_connectivity == "online") {
            throw IllegalStateException("UI Tests called in online flavor - to prevent upstream pain we fail here fast now. Please test ui in offline flavor.")
        }
    }

    override fun testAppClass() = TestApp::class.java

}
