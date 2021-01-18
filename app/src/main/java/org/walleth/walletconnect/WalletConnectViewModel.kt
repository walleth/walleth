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

    fun processURI(_uri: String): Boolean {
        if (uri != _uri) {
            uri = _uri

            val config = fromWCUri(_uri)

            val fullyQualifiedConfig = if (config.isFullyQualifiedConfig()) {
                config.toFullyQualifiedConfig()
            } else {
                sessionStore.load(config.handshakeTopic)?.config
            }

            if (fullyQualifiedConfig != null) {
                session = WCSession(
                        fullyQualifiedConfig,
                        MoshiPayloadAdapter(moshi),
                        sessionStore,
                        OkHttpTransport.Builder(okHttpClient, moshi),
                        Session.PeerMeta(name = "WallETH")
                )

                viewModelScope.launch {
                    session?.init()
                }
                return true
            }
        }
        return false
    }

    var statusText: String? = null
    var iconURL: String? = null
    var showSwitchNetworkButton = false
    var showSwitchAccountButton = false
    var peerMeta: Session.PeerMeta? = null

}