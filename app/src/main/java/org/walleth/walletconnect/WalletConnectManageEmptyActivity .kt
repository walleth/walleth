package org.walleth.walletconnect

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_empty_wc_manage.*
import org.koin.android.ext.android.inject
import org.walletconnect.impls.WCSessionStore
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity

class WalletConnectManageEmptyActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty_wc_manage)
        enter_wc_url.setOnClickListener {
            showWalletConnectURLInputAlert()
        }
    }

}

