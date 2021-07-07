package org.walleth.trezor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.squareup.wire.Message
import io.trezor.deviceprotocol.EthereumMessageSignature
import io.trezor.deviceprotocol.EthereumSignMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.ByteString.Companion.toByteString
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.komputing.kbip44.BIP44
import org.komputing.khex.extensions.hexToByteArray
import org.komputing.khex.extensions.toHexString
import org.komputing.khex.model.HexString
import org.ligi.kaxtui.alert
import org.walleth.R.string
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.addresses.getTrezorDerivationPath

private fun Activity.getTrezorSignTextIntent() = Intent(this, TrezorSignTextActivity::class.java)

fun Activity.getTrezorSignTextIntent(msg: String, address: Address) = getTrezorSignTextIntent().putExtra("MSG", msg).putExtra("Address", address.hex)

class TrezorSignTextActivity : BaseTrezorActivity() {

    private val msg by lazy {
        intent.getStringExtra("MSG")
                ?: throw(java.lang.IllegalArgumentException("no MSG in Intent"))
    }

    private val wantedAddress by lazy {
        Address(intent.getStringExtra("Address")
                ?: throw(java.lang.IllegalArgumentException("no Address in Intent")))
    }
    private val currentAddressProvider: CurrentAddressProvider by inject()

    override fun handleAddress(address: Address) {
        if (address != wantedAddress) {
            val messageTemplate = if (isKeepKeyDevice) string.keepkey_reported_different_address else string.trezor_reported_different_address
            val message = getString(messageTemplate, address, wantedAddress)
            alert(message) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        } else {
            enterState(STATES.PROCESS_TASK)
        }
    }

    override suspend fun getTaskSpecificMessage() = if (isKeepKeyDevice) {
        TODO("handle KeepKey")
    } else {
        EthereumSignMessage.Builder()
                .message(HexString(msg).hexToByteArray().toByteString())
                .address_n(currentBIP44!!.path.map { it.numberWithHardeningFlag })
                .build()
    }


    override fun handleExtraMessage(res: Message<*, *>?) {
        if (res is EthereumMessageSignature) {
            lifecycleScope.launch(Dispatchers.Main) {
                setResult(RESULT_OK, Intent().apply { putExtra("HEX", res.signature.toByteArray().toHexString()) })
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.subtitle = getString(if (isKeepKeyDevice) string.activity_subtitle_sign_with_keepkey else string.activity_subtitle_sign_with_trezor)

        lifecycleScope.launch {
            currentBIP44 = appDatabase.addressBook.byAddress(currentAddressProvider.getCurrentNeverNull())?.getTrezorDerivationPath()?.let { trezorDerivationPath ->
                BIP44(trezorDerivationPath)
            } ?: throw IllegalArgumentException("Starting TREZOR Activity without derivation path")

            connectAndExecute()
        }
    }

}
