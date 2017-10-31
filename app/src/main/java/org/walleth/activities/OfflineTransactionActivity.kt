package org.walleth.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_relay.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.json.JSONObject
import org.kethereum.eip155.extractChainID
import org.kethereum.functions.encodeRLP
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.*
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.qrscan.startScanActivityForResult
import org.walleth.data.AppDatabase
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.kethereum.geth.extractSignatureData
import org.walleth.kethereum.geth.toBigInteger
import org.walleth.kethereum.geth.toKethereumAddress
import org.walleth.khex.clean0xPrefix
import org.walleth.khex.hexToByteArray
import org.walleth.khex.toHexString
import java.math.BigInteger

private const val KEY_CONTENT = "KEY_OFFLINE_TX_CONTENT"

fun Context.getOfflineTransactionIntent(content: String) = Intent(this, OfflineTransactionActivity::class.java).apply {
    putExtra(KEY_CONTENT, content)
}

fun String.isUnsignedTransactionJSON() = try {
    JSONObject(this).let {
        it.has("to") && it.has("from") && it.has("chainId") && it.has("nonce") && it.has("value")
                && it.has("gasLimit") && it.has("gasPrice") && it.has("data") && it.has("nonce")
    }
} catch (e: Exception) {
    false
}

fun String.isSignedTransactionJSON() = try {
    JSONObject(this).let {
        it.has("signedTransactionRLP") && it.has("chainId")
    }
} catch (e: Exception) {
    false
}


class OfflineTransactionActivity : AppCompatActivity() {

    private val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()
    private val appDatabase: AppDatabase by LazyKodein(appKodein).instance()
    private val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_relay)

        supportActionBar?.subtitle = getString(R.string.relay_transaction)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fab.setOnClickListener {
            execute()
        }

        intent.getStringExtra(KEY_CONTENT)?.let {
            transaction_to_relay_hex.setText(it)
            execute()
        }

    }

    private fun execute() {
        val content = transaction_to_relay_hex.text.toString()
        if (content.isUnsignedTransactionJSON()) {
            val json = JSONObject(content)
            val from = json.getString("from")
            val currentAccount = currentAddressProvider.getCurrent().hex
            if (from.clean0xPrefix().toLowerCase() != currentAccount.clean0xPrefix().toLowerCase()) {
                alert("The from field of the transaction ($from) does not match your current account ($currentAccount)")
                return
            }
            val chainId = json.getLong("chainId")
            if (chainId != networkDefinitionProvider.getCurrent().chain.id) {
                alert("The chainId of the transaction ($chainId) does not match your current chainId")
                return
            }

            val to = json.getString("to")
            val nonce = json.getString("nonce")
            val value = json.getString("value")
            val gasLimit = json.getString("gasLimit")
            val gasPrice = json.getString("gasPrice")
            val data = json.getString("data")

            val transaction = createTransactionWithDefaults(
                    value = BigInteger(value.clean0xPrefix(), 16),
                    from = Address(from),
                    to = Address(to),
                    chain = ChainDefinition(chainId),
                    nonce = BigInteger(nonce.clean0xPrefix(), 16),
                    gasLimit = BigInteger(gasLimit.clean0xPrefix(), 16),
                    gasPrice = BigInteger(gasPrice.clean0xPrefix(), 16),
                    input = data.hexToByteArray().toList(),
                    creationEpochSecond = System.currentTimeMillis() / 1000
            )


            transaction.txHash = transaction.encodeRLP().keccak().toHexString("0x")

            createTransaction(transaction, null)

        } else if (content.isSignedTransactionJSON()) {
            val json = JSONObject(content)

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
        } else {
            executeForRLP()
        }
    }

    private fun executeForRLP() {
        try {
            val transactionRLP = transaction_to_relay_hex.text.toString().hexToByteArray()
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

    private fun createTransaction(transaction: Transaction, signatureData: SignatureData?) {
        async(UI) {
            try {

                async(CommonPool) {

                    val transactionState = TransactionState(needsSigningConfirmation = signatureData == null)

                    appDatabase.transactions.upsert(transaction.toEntity(signatureData, transactionState))

                }.await()

                startActivity(getTransactionActivityIntentForHash(transaction.txHash!!))
                finish()


            } catch (e: Exception) {
                alert("Problem " + e.message)
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_offline_transaction, menu)
        return super.onCreateOptionsMenu(menu)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {


        resultData?.let {
            if (it.hasExtra("SCAN_RESULT")) {
                transaction_to_relay_hex.setText(it.getStringExtra("SCAN_RESULT"))
            }
        }


    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.menu_scan -> {
            startScanActivityForResult(this)
            true
        }

        android.R.id.home -> {
            finish()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }
}
