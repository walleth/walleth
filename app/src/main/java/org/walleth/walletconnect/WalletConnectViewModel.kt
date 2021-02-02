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

    var showSwitchNetworkButton = false
    var showSwitchAccountButton = false
    var peerMeta: Session.PeerMeta? = null

}