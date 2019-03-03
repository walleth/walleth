package org.walleth.viewmodels

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.squareup.moshi.Moshi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.walletconnect.Session
import org.walletconnect.Session.Config.Companion.fromWCUri
import org.walletconnect.impls.MoshiPayloadAdapter
import org.walletconnect.impls.OkHttpTransport
import org.walletconnect.impls.WCSession
import org.walletconnect.impls.WCSessionStore

class WalletConnectViewModel(val app: Application,
                             val moshi: Moshi,
                             val sessionStore: WCSessionStore) : AndroidViewModel(app) {

    var session: WCSession? = null
    var uri: String? = null

    fun processURI(_uri: String) = if (uri != _uri) {
        uri = _uri
        session = WCSession(
                fromWCUri(_uri),
                MoshiPayloadAdapter(moshi),
                sessionStore,
                OkHttpTransport.Builder(OkHttpClient.Builder().build(), moshi),
                Session.PayloadAdapter.PeerMeta(name = "WallETH")
        )

        GlobalScope.launch {
            session?.init()
        }
        true
    } else {
        false
    }

    var statusText: String? = null
    var showSwitchNetworkButton = false
    var showSwitchAccountButton = false
    var peerMeta: Session.PayloadAdapter.PeerMeta? = null

}