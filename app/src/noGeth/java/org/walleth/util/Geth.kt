package org.walleth.util

import org.json.JSONObject
import org.walleth.activities.OfflineTransactionActivity
import org.walleth.data.keystore.NoGethWallethKeyStore
import org.walleth.ui.WalletPrefsFragment

internal fun setGethVerbosity(verbosity: Long) {
    // do nothing, see withGeth for implementation
}

internal fun OfflineTransactionActivity.executeSignedForRLP(json: JSONObject) {
    // do nothing, see withGeth for implementation
}

internal fun OfflineTransactionActivity.executeForRLP(transactionRLP: ByteArray) {
    // do nothing, see withGeth for implementation
}

internal fun WalletPrefsFragment.toggleGethLightEthereumService(key: String) {
    // do nothing, see withGeth for implementation
}

internal fun WalletPrefsFragment.setGethPreferenceVisibility() {
    // do nothing, see withGeth for implementation
}

internal fun createKeyStore() = NoGethWallethKeyStore()
