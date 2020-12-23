package org.walleth.walletconnect

import android.content.Intent
import android.os.Bundle
import org.ligi.kaxtui.alert
import org.walleth.base_activities.BaseSubActivity
import java.lang.IllegalArgumentException


class WalletConnectErrorActivity : BaseSubActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alert(intent.getStringExtra(Intent.EXTRA_TEXT)?:throw IllegalArgumentException("no EXTRA_TEXT in intent")) {
            finish()
        }
    }
}
