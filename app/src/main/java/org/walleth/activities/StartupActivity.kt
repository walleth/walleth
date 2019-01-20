package org.walleth.activities

import android.os.Bundle
import org.koin.android.ext.android.inject
import org.ligi.kaxt.livedata.nonNull
import org.ligi.kaxt.livedata.observe
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.data.networks.CurrentAddressProvider

class StartupActivity : BaseSubActivity() {

    val currentAddressProvider: CurrentAddressProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.creating_key_busy_indicator)

        currentAddressProvider.nonNull().observe(this) {
            startActivityFromClass(MainActivity::class.java)
            finish()
        }
    }

}
