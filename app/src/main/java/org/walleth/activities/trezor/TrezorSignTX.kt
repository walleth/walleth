package org.walleth.activities.trezor

import android.content.Context
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.os.Bundle
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import org.kethereum.bip44.BIP44
import org.kethereum.model.Address
import org.kethereum.model.SignatureData
import org.ligi.kaxtui.alert
import org.walleth.R.string
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.TransactionWithState
import org.walleth.kethereum.android.TransactionParcel
import org.walleth.khex.hexToByteArray
import java.math.BigInteger


fun Context.startTrezorActivity(transactionParcel: TransactionParcel) {
    startActivity(Intent(this, TrezorSignTX::class.java).putExtra("TX", transactionParcel))
}

class TrezorSignTX : BaseTrezorActivity() {

    override fun handleAddress(address: Address) {
        if (address != transaction.from) {
            alert("TREZOR reported different source Address. $address is not ${transaction.from}", onOKListener = OnClickListener { _, _ ->
                finish()
            })
        } else {
            enterNewState(STATES.PROCESS_TASK)
        }
    }

    override fun getTaskSpecificMessage() = TrezorMessage.EthereumSignTx.newBuilder()
            .setTo(ByteString.copyFrom(transaction.to!!.hex.hexToByteArray()))
            .setValue(ByteString.copyFrom(transaction.value.toByteArray().removeLeadingZero()))
            .setNonce(ByteString.copyFrom(transaction.nonce!!.toByteArray().removeLeadingZero()))
            .setGasPrice(ByteString.copyFrom(transaction.gasPrice.toByteArray().removeLeadingZero()))
            .setGasLimit(ByteString.copyFrom(transaction.gasLimit.toByteArray().removeLeadingZero()))
            .setChainId(networkDefinitionProvider.currentDefinition.chainId.toInt())
            .setDataLength(transaction.input.size)
            .setDataInitialChunk(ByteString.copyFrom(transaction.input.toByteArray()))
            .addAllAddressN(currentBIP44!!.toIntList())
            .build()!!


    override fun handleExtraMessage(res: Message?) {
        if (res is TrezorMessage.EthereumTxRequest) {

            transaction.signatureData = SignatureData(
                    r = BigInteger(res.signatureR.toByteArray()),
                    s = BigInteger(res.signatureS.toByteArray()),
                    v = res.signatureV.toByte()
            )

            transactionProvider.addPendingTransaction(TransactionWithState(transaction, TransactionState()))

            finish()
        }
    }

    val transaction by lazy { intent.getParcelableExtra<TransactionParcel>("TX").transaction }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentBIP44 = addressBook.getEntryForName(keyStore.getCurrentAddress())?.trezorDerivationPath?.let {
            BIP44.fromPath(it)
        } ?: throw IllegalArgumentException("Starting TREZOR Activity")
        supportActionBar?.subtitle = getString(string.activity_subtitle_sign_with_trezor)
    }

    private fun ByteArray.removeLeadingZero() = if (first() == 0.toByte()) copyOfRange(1, size) else this


}
