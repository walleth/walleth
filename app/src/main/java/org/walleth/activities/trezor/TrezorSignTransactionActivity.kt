package org.walleth.activities.trezor

import android.app.Activity
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.os.Bundle
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.kethereum.bip44.BIP44
import org.kethereum.model.Address
import org.kethereum.model.SignatureData
import org.ligi.kaxtui.alert
import org.ligi.kroom.inTransaction
import org.walleth.R
import org.walleth.R.string
import org.walleth.data.addressbook.getByAddressAsync
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.kethereum.android.TransactionParcel
import org.walleth.khex.hexToByteArray
import java.math.BigInteger

const val TREZOR_REQUEST_CODE = 7688

fun Activity.startTrezorActivity(transactionParcel: TransactionParcel) {
    val trezorIntent = Intent(this, TrezorSignTransactionActivity::class.java).putExtra("TX", transactionParcel)
    startActivityForResult(trezorIntent, TREZOR_REQUEST_CODE)
}

class TrezorSignTransactionActivity : BaseTrezorActivity() {

    private val transaction by lazy { intent.getParcelableExtra<TransactionParcel>("TX") }
    private val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()

    override fun handleAddress(address: Address) {
        if (address != transaction.transaction.from) {
            val message = getString(R.string.trezor_reported_different_address, address, transaction.transaction.from)
            alert(message, onOKListener = OnClickListener { _, _ ->
                setResult(Activity.RESULT_CANCELED)
                finish()
            })
        } else {
            enterNewState(STATES.PROCESS_TASK)
        }
    }

    override fun getTaskSpecificMessage() = TrezorMessage.EthereumSignTx.newBuilder()
            .setTo(ByteString.copyFrom(transaction.transaction.to!!.hex.hexToByteArray()))
            .setValue(ByteString.copyFrom(transaction.transaction.value.toByteArray().removeLeadingZero()))
            .setNonce(ByteString.copyFrom(transaction.transaction.nonce!!.toByteArray().removeLeadingZero()))
            .setGasPrice(ByteString.copyFrom(transaction.transaction.gasPrice.toByteArray().removeLeadingZero()))
            .setGasLimit(ByteString.copyFrom(transaction.transaction.gasLimit.toByteArray().removeLeadingZero()))
            .setChainId(networkDefinitionProvider.value!!.chain.id.toInt())
            .setDataLength(transaction.transaction.input.size)
            .setDataInitialChunk(ByteString.copyFrom(transaction.transaction.input.toByteArray()))
            .addAllAddressN(currentBIP44!!.toIntList())
            .build()!!


    override fun handleExtraMessage(res: Message?) {
        if (res is TrezorMessage.EthereumTxRequest) {

            val oldHash = transaction.transaction.txHash
            val signatureData = SignatureData(
                    r = BigInteger(res.signatureR.toByteArray()),
                    s = BigInteger(res.signatureS.toByteArray()),
                    v = res.signatureV.toByte()
            )
            async(UI) {
                async(CommonPool) {
                    appDatabase.inTransaction {
                        oldHash?.let { transactions.deleteByHash(it) }
                        transactions.upsert(transaction.transaction.toEntity(signatureData, TransactionState()))
                    }
                }.await()
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        appDatabase.addressBook.getByAddressAsync(currentAddressProvider.getCurrent()) {
            currentBIP44 = it?.trezorDerivationPath?.let { BIP44.fromPath(it) } ?: throw IllegalArgumentException("Starting TREZOR Activity")
            handler.post(mainRunnable)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.subtitle = getString(string.activity_subtitle_sign_with_trezor)
    }

    private fun ByteArray.removeLeadingZero() = if (first() == 0.toByte()) copyOfRange(1, size) else this


}
