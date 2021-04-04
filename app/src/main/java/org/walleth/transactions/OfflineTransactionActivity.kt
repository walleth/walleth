package org.walleth.transactions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_relay.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.kethereum.eip155.extractChainID
import org.kethereum.eip155.extractFrom
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.extensions.toHexString
import org.kethereum.extensions.transactions.toTransaction
import org.kethereum.extensions.transactions.toTransactionSignatureData
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.ChainId
import org.kethereum.model.SignatureData
import org.kethereum.model.Transaction
import org.kethereum.rlp.*
import org.koin.android.ext.android.inject
import org.komputing.khex.extensions.clean0xPrefix
import org.komputing.khex.extensions.hexToByteArray
import org.komputing.khex.extensions.toHexString
import org.komputing.khex.model.HexString
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.chains.ChainInfoProvider
import org.walleth.chains.chainIDAlert
import org.walleth.data.AppDatabase
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.qr.scan.getQRScanActivity
import org.walleth.sign.ParitySignerQRActivity
import org.walleth.util.isParityUnsignedTransactionJSON
import org.walleth.util.isSignedTransactionJSON
import org.walleth.util.isUnsignedTransactionJSON

private const val KEY_CONTENT = "KEY_OFFLINE_TX_CONTENT"

fun Context.getOfflineTransactionIntent(content: String) = Intent(this, OfflineTransactionActivity::class.java).apply {
    putExtra(KEY_CONTENT, content)
}

class OfflineTransactionActivity : BaseSubActivity() {

    private val chainInfoProvider: ChainInfoProvider by inject()
    private val appDatabase: AppDatabase by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()

    private val scanQRForResult: ActivityResultLauncher<Intent> = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val result = it.data?.getStringExtra("SCAN_RESULT")
            transaction_to_relay_hex.setText(result)
            if (result?.isUnsignedTransactionJSON() == true || result?.isParityUnsignedTransactionJSON() == true) {
                execute()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_relay)

        supportActionBar?.subtitle = getString(R.string.relay_transaction)

        fab.setOnClickListener {
            execute()
        }

        parity_signer_button.setOnClickListener {
            startActivityFromClass(ParitySignerQRActivity::class.java)
        }

        intent.getStringExtra(KEY_CONTENT)?.let {
            if (it.isNotEmpty()) {
                transaction_to_relay_hex.setText(it)
                execute()
            }
        }

    }

    private fun execute() {
        lifecycleScope.launch(Dispatchers.Main) {
            val content = transaction_to_relay_hex.text.toString()
            when {
                content.isUnsignedTransactionJSON() -> handleUnsignedTransactionJson(content)
                content.isParityUnsignedTransactionJSON() -> handleParityUnsignedTransactionJson(content)
                content.isSignedTransactionJSON() -> {
                    val json = JSONObject(content)

                    try {
                        val transactionRLP = HexString(json.getString("signedTransactionRLP")).hexToByteArray()
                        val txRLP = transactionRLP.decodeRLP() as? RLPList
                                ?: throw IllegalArgumentException("RLP not a list")
                        require(txRLP.element.size == 9) { "RLP list has the wrong size ${txRLP.element.size} != 9" }


                        val signatureData = txRLP.toTransactionSignatureData()
                        val transaction = txRLP.toTransaction()
                                ?: throw IllegalArgumentException("RLP list has the wrong size ${txRLP.element.size} != 9")

                        val chainID = (signatureData.extractChainID()
                                ?: throw IllegalArgumentException("Cannot extract chainID from RLP"))
                        transaction.chain = chainID

                        transaction.from = transaction.extractFrom(signatureData, ChainId(chainID))
                        transaction.txHash = txRLP.encode().keccak().toHexString()
                        createTransaction(transaction, signatureData)
                    } catch (e: Exception) {
                        alert(getString(R.string.input_not_valid_message, e.message), getString(R.string.input_not_valid_title))
                    }

                }
                else -> executeForRLP()
            }
        }
    }

    private fun handleParityUnsignedTransactionJson(content: String) {
        val json = JSONObject(content)

        val dataJSON = json.getJSONObject("data")
        val rlp = HexString(dataJSON.getString("rlp")).hexToByteArray().decodeRLP()
        if (rlp is RLPList) {
            if (rlp.element.size != 9) {
                alert("Invalid RLP list - has size " + rlp.element.size + " should have 9")
                return
            }

            val transaction = rlp.toTransaction()

            val chainId = (rlp.element[6] as RLPElement).toUnsignedBigIntegerFromRLP()

            chainIDAlert(chainInfoProvider, appDatabase, ChainId(chainId.toLong())) {

                if (transaction == null) {
                    alert("could not decode transaction")
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        handleUnsignedTransaction(
                                from = "0x" + HexString(dataJSON.getString("account")).clean0xPrefix().string,
                                to = transaction.to!!.hex,
                                data = transaction.input.toHexString(),
                                value = transaction.value!!.toHexString(),
                                nonce = transaction.nonce!!.toHexString(),
                                gasPrice = transaction.gasPrice!!.toHexString(),
                                gasLimit = transaction.gasLimit!!.toHexString(),
                                chainId = chainInfoProvider.getCurrentChainId(),
                                parityFlow = true
                        )
                    }
                }
            }

        } else {
            alert("Invalid RLP")
        }
    }

    private suspend fun handleUnsignedTransactionJson(content: String) {
        val json = JSONObject(content)
        handleUnsignedTransaction(
                from = json.getString("from"),
                chainId = ChainId(json.getLong("chainId")),
                to = json.getString("to"),
                gasLimit = json.getString("gasLimit"),
                value = json.getString("value"),
                nonce = json.getString("nonce"),
                data = json.getString("data"),
                gasPrice = json.getString("gasPrice"),
                parityFlow = false
        )
    }

    private suspend fun handleUnsignedTransaction(from: String,
                                                  chainId: ChainId,
                                                  to: String,
                                                  value: String,
                                                  gasLimit: String,
                                                  nonce: String,
                                                  data: String,
                                                  gasPrice: String,
                                                  parityFlow: Boolean) {

        val currentAccount = currentAddressProvider.getCurrentNeverNull().hex
        if (HexString(from).clean0xPrefix().string.equals(HexString(currentAccount).clean0xPrefix().string, ignoreCase = true)) {
            alert("The from field of the transaction ($from) does not match your current account ($currentAccount)")
            return
        }

        if (chainId != chainInfoProvider.getCurrentChainId()) {
            alert("The chainId of the transaction ($chainId) does not match your current chainId")
            return
        }

        val url = ERC681(scheme = "ethereum",
                address = to,
                value = HexString(value).hexToBigInteger(),
                gasLimit = HexString(gasLimit).hexToBigInteger(),
                chainId = chainId
        ).generateURL()

        startActivity(Intent(this, CreateTransactionActivity::class.java).apply {
            setData(Uri.parse(url))
            putExtra("nonce", nonce)
            putExtra("data", data)
            putExtra("gasPrice", gasPrice)
            putExtra("from", from)
            putExtra("parityFlow", parityFlow)
        })
    }

    private suspend fun executeForRLP() {

        try {
            val transactionRLP = HexString(transaction_to_relay_hex.text.toString()).hexToByteArray()

            val rlp = transactionRLP.decodeRLP()

            val rlpList = rlp as RLPList

            if (rlpList.element.size != 9) {
                alert("Found RLP without signature - this is not supported anymore - the transaction source must be in JSON and include the chainID")
            } else {

                val signatureData = rlpList.toTransactionSignatureData()
                val transaction = rlpList.toTransaction()?.apply {
                    txHash = rlpList.encode().keccak().toHexString()
                }

                ERC681(address = transaction?.to?.hex)


                val extractChainID = signatureData.extractChainID()
                val chainId = extractChainID?.toLong()?.let { ChainId(it) } ?: chainInfoProvider.getCurrentChainId()

                transaction?.chain = chainId.value
                transaction?.let {
                    createTransaction(it, signatureData)
                }
            }

        } catch (e: Exception) {
            alert(getString(R.string.input_not_valid_message, e.message), getString(R.string.input_not_valid_title))
        }
    }

    private fun createTransaction(transaction: Transaction,
                                  signatureData: SignatureData?) = lifecycleScope.launch(Dispatchers.Main) {
        try {

            withContext(Dispatchers.Default) {

                val transactionState = TransactionState(needsSigningConfirmation = signatureData == null)

                appDatabase.transactions.upsert(transaction.toEntity(signatureData, transactionState))

            }

            startActivity(getTransactionActivityIntentForHash(transaction.txHash!!))
            finish()

        } catch (e: Exception) {
            alert("Problem " + e.message)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_offline_transaction, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.menu_scan -> true.also {
            scanQRForResult.launch(getQRScanActivity())
        }
        else -> super.onOptionsItemSelected(item)
    }
}
