package org.walleth.walletconnect

import android.content.Intent
import android.net.Uri
import org.kethereum.erc1328.isERC1328
import org.kethereum.erc831.toERC831
import org.kethereum.model.EthereumURI
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.util.showInputAlert

fun BaseSubActivity.showWalletConnectURLInputAlert() {
    showInputAlert(R.string.enter_wc_url) {
        urlString ->

        val ethereumURI = EthereumURI(urlString).toERC831()

        if (ethereumURI.isERC1328()) {
            val wcIntent = Intent(this, WalletConnectConnectionActivity::class.java)
            wcIntent.data = Uri.parse(urlString)
            startActivity(wcIntent)
            finish()
        } else {
            alert("Expected ERC1328 - but got $urlString")
        }
    }
}