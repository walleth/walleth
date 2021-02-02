package org.walleth.walletconnect

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.walletconnect.Session
import org.walletconnect.impls.MoshiPayloadAdapter
import org.walletconnect.impls.OkHttpTransport
import org.walletconnect.impls.WCSession
import org.walletconnect.impls.WCSessionStore

class WalletConnectHandler(private val moshi: Moshi,
                           private val okHttpClient: OkHttpClient,
                           private val sessionStore: WCSessionStore) {

    var session: WCSession? = null
    var uri: String? = null

    fun processURI(_uri: String): Boolean {
        if (uri != _uri) {
            uri = _uri

            val config = Session.Config.fromWCUri(_uri)

            val fullyQualifiedConfig = sessionStore.load(config.handshakeTopic)?.config
                    ?: if (config.isFullyQualifiedConfig()) {
                        config.toFullyQualifiedConfig()
                    } else null

            if (fullyQualifiedConfig != null) {
                session = WCSession(
                        fullyQualifiedConfig,
                        MoshiPayloadAdapter(moshi),
                        sessionStore,
                        OkHttpTransport.Builder(okHttpClient, moshi),
                        Session.PeerMeta(name = "WallETH")
                )

                session?.init()

                return true
            }
        }
        return false
    }

}