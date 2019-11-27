package org.walleth.walletconnect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.walletconnect.Session
import org.walletconnect.Session.Config.Companion.fromWCUri
import org.walletconnect.impls.MoshiPayloadAdapter
import org.walletconnect.impls.OkHttpTransport
import org.walletconnect.impls.WCSession
import org.walletconnect.impls.WCSessionStore

class WalletConnectViewModel(app: Application,
                             private val moshi: Moshi,
                             private val okHttpClient: OkHttpClient,
                             private val sessionStore: WCSessionStore) : AndroidViewModel(app) {

    var session: WCSession? = null
    var uri: String? = null

    fun processURI(_uri: String) = if (uri != _uri) {
        uri = _uri
        session = WCSession(
                fromWCUri(_uri),
                MoshiPayloadAdapter(moshi),
                sessionStore,
                OkHttpTransport.Builder(okHttpClient, moshi),
                Session.PeerMeta(name = "WallETH")
        )

        viewModelScope.launch {
            session?.init()
        }
        true
    } else {
        false
    }

    var statusText: String? = null
    var iconURL: String? = null
    var showSwitchNetworkButton = false
    var showSwitchAccountButton = false
    var peerMeta: Session.PeerMeta? = null

}