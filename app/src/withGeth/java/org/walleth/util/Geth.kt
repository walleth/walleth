package org.walleth.util

import android.app.AlertDialog
import android.content.Intent
import android.support.v7.preference.CheckBoxPreference
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.json.JSONObject
import org.kethereum.eip155.extractChainID
import org.kethereum.model.ChainDefinition
import org.kethereum.model.createTransactionWithDefaults
import org.ligi.kaxtui.alert
import org.walleth.App
import org.walleth.R
import org.walleth.activities.OfflineTransactionActivity
import org.walleth.geth.services.GethLightEthereumService
import org.walleth.geth.services.GethLightEthereumService.Companion.gethStopIntent
import org.walleth.kethereum.geth.extractSignatureData
import org.walleth.kethereum.geth.toBigInteger
import org.walleth.kethereum.geth.toKethereumAddress
import org.walleth.khex.hexToByteArray
import org.walleth.ui.WalletPrefsFragment
import java.math.BigInteger


internal fun setGethVerbosity(verbosity: Long) {
    Geth.setVerbosity(verbosity)
}

internal fun OfflineTransactionActivity.executeSignedForRLP(json: JSONObject) {
    try {
        val transactionRLP = json.getString("signedTransactionRLP").hexToByteArray()
        val gethTransaction = Geth.newTransactionFromRLP(transactionRLP)
        val signatureData = gethTransaction.extractSignatureData()

        if (signatureData == null) {
            alert("Found unsigned TX - but must be signed")
        } else {
            val extractChainID = signatureData.extractChainID()
            val chainId = if (extractChainID == null) {
                BigInt(networkDefinitionProvider.getCurrent().chain.id)
            } else {
                BigInt(extractChainID.toLong())
            }
            val transaction = createTransactionWithDefaults(
                    value = BigInteger(gethTransaction.value.toString()),
                    from = gethTransaction.getFrom(chainId).toKethereumAddress(),
                    to = gethTransaction.to!!.toKethereumAddress(),
                    chain = ChainDefinition(chainId.toBigInteger().toLong()),
                    nonce = BigInteger(gethTransaction.nonce.toString()),
                    creationEpochSecond = System.currentTimeMillis() / 1000,
                    txHash = gethTransaction.hash.hex
            )
            createTransaction(transaction, signatureData)
        }
    } catch (e: Exception) {
        alert(getString(R.string.input_not_valid_message, e.message), getString(R.string.input_not_valid_title))
    }
}

internal fun OfflineTransactionActivity.executeForRLP(transactionRLP: ByteArray) {
    try {
        val gethTransaction = Geth.newTransactionFromRLP(transactionRLP)
        val signatureData = gethTransaction.extractSignatureData()

        if (signatureData == null) {
            alert("Found RLP without signature - this is not supported anymore - the transaction source must be in JSON and include the chainID")
        } else {
            val extractChainID = signatureData.extractChainID()
            val chainId = if (extractChainID == null) {
                BigInt(networkDefinitionProvider.getCurrent().chain.id)
            } else {
                BigInt(extractChainID.toLong())
            }
            val transaction = createTransactionWithDefaults(
                    value = BigInteger(gethTransaction.value.toString()),
                    from = gethTransaction.getFrom(chainId).toKethereumAddress(),
                    to = gethTransaction.to!!.toKethereumAddress(),
                    chain = ChainDefinition(chainId.toBigInteger().toLong()),
                    nonce = BigInteger(gethTransaction.nonce.toString()),
                    creationEpochSecond = System.currentTimeMillis() / 1000,
                    txHash = gethTransaction.hash.hex
            )
            createTransaction(transaction, signatureData)
        }
    } catch (e: Exception) {
        alert(getString(R.string.input_not_valid_message, e.message), getString(R.string.input_not_valid_title))
    }
}

internal fun WalletPrefsFragment.toggleGethLightEthereumService(key: String) {
    if ((findPreference(key) as CheckBoxPreference).isChecked != GethLightEthereumService.isRunning) {
        if (GethLightEthereumService.isRunning) {
            context.startService(context.gethStopIntent())
        } else {
            context.startService(Intent(context, GethLightEthereumService::class.java))
        }
        async(UI) {
            val alert = AlertDialog.Builder(getContext())
                    .setCancelable(false)
                    .setMessage(R.string.settings_please_wait).show()
            async(CommonPool) {
                while (GethLightEthereumService.isRunning != GethLightEthereumService.shouldRun) {
                    delay(100)
                }
            }.await()
            alert.dismiss()
        }
    }
}

internal fun WalletPrefsFragment.setGethPreferenceVisibility() {
    findPreference(getString(R.string.key_prefs_start_light)).isVisible = false
}

internal fun App.createKeyStore() = org.walleth.data.keystore.GethBackedWallethKeyStore(this)