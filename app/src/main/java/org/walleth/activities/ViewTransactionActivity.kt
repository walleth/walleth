package org.walleth.activities

import android.arch.lifecycle.Observer
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_view_transaction.*
import kotlinx.coroutines.*
import org.kethereum.functions.encodeRLP
import org.kethereum.functions.getTokenTransferTo
import org.kethereum.functions.getTokenTransferValue
import org.kethereum.functions.isTokenTransfer
import org.koin.android.ext.android.inject
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromURL
import org.walleth.R
import org.walleth.contracts.FourByteDirectory
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.resolveNameAsync
import org.walleth.data.blockexplorer.BlockExplorerProvider
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.tokens.getRootTokenForChain
import org.walleth.data.transactions.TransactionEntity
import org.walleth.functions.setQRCode
import org.walleth.functions.toHexString
import org.walleth.khex.toHexString
import org.walleth.ui.valueview.ValueViewController

private const val HASH_KEY = "TXHASH"
fun Context.getTransactionActivityIntentForHash(hex: String) = Intent(this, ViewTransactionActivity::class.java).apply {
    putExtra(HASH_KEY, hex)
}

class ViewTransactionActivity : BaseSubActivity() {

    private val appDatabase: AppDatabase by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val blockExplorerProvider: BlockExplorerProvider by inject()
    private val networkDefinitionProvider: NetworkDefinitionProvider by inject()
    private val exchangeRateProvider: ExchangeRateProvider by inject()
    private val settings: Settings by inject()

    private var txEntity: TransactionEntity? = null
    private val fourByteDirectory: FourByteDirectory by inject()

    private val amountViewModel by lazy {
        ValueViewController(value_view, exchangeRateProvider, settings)
    }

    private val feeViewModel by lazy {
        ValueViewController(fee_value_view, exchangeRateProvider, settings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_view_transaction)
    }

    override fun onResume() {
        super.onResume()

        appDatabase.transactions.getByHashLive(intent.getStringExtra(HASH_KEY)).observe(this, Observer<TransactionEntity> { txEntry ->
            if (txEntry != null) {
                txEntity = txEntry
                invalidateOptionsMenu()
                val transaction = txEntry.transaction

                supportActionBar?.subtitle = getString(R.string.transaction_subtitle)

                nonce.text = transaction.nonce.toString()
                event_log_textview.text = txEntry.transactionState.eventLog

                fab.setVisibility(txEntry.transactionState.needsSigningConfirmation)
                fab.setOnClickListener {
                    GlobalScope.launch(Dispatchers.Main) {
                        launch(Dispatchers.Default) {
                            txEntry.transactionState.needsSigningConfirmation = false
                            appDatabase.transactions.upsert(txEntry)
                        }

                        finish()
                    }
                }

                feeViewModel.setValue(txEntry.transaction.gasLimit!! * txEntry.transaction.gasPrice!!, getRootTokenForChain(networkDefinitionProvider.getCurrent()))

                val relevantAddress = if (transaction.from == currentAddressProvider.getCurrent()) {
                    from_to_title.setText(R.string.transaction_to_label)
                    if (transaction.isTokenTransfer()) {
                        transaction.getTokenTransferTo()
                    } else {
                        transaction.to
                    }
                } else {
                    from_to_title.setText(R.string.transaction_from_label)
                    txEntry.transaction.from
                }

                relevantAddress?.let { ensured_relevant_address ->
                    appDatabase.addressBook.resolveNameAsync(ensured_relevant_address) { name ->
                        from_to.text = name

                        add_address.setVisibility(name == ensured_relevant_address.hex)
                    }

                    add_address.setOnClickListener {
                        startCreateAccountActivity(ensured_relevant_address.hex)
                    }

                    copy_address.setOnClickListener {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(getString(R.string.ethereum_address), ensured_relevant_address.hex)
                        clipboard.primaryClip = clip
                        Snackbar.make(fab, R.string.snackbar_after_address_copy, Snackbar.LENGTH_LONG).show()
                    }
                }

                advanced_button.setOnClickListener {
                    advanced_container.visibility = View.VISIBLE
                    advanced_button.visibility = View.GONE
                }


                if (txEntry.transactionState.isPending && !txEntry.transactionState.needsSigningConfirmation && (!txEntry.transactionState.relayed.isNotEmpty())) {
                    rlp_header.visibility = View.VISIBLE
                    rlp_image.visibility = View.VISIBLE

                    if (txEntry.signatureData != null) {
                        rlp_header.setText(R.string.signed_rlp_header_text)
                        rlp_image.setQRCode("""{
                            "signedTransactionRLP":"${txEntry.transaction.encodeRLP(txEntry.signatureData).toHexString()}",
                            "chainId":${txEntry.transaction.chain}
                            }""")
                    } else {
                        rlp_header.setText(R.string.unsigned_rlp_header_text)

                        rlp_image.setQRCode("""{
"nonce":"${txEntry.transaction.nonce?.toHexString()}","gasPrice":"${txEntry.transaction.gasPrice!!.toHexString()}","gasLimit":"${txEntry.transaction.gasLimit!!.toHexString()}","to":"${txEntry.transaction.to}","from":"${txEntry.transaction.from}","value":"${txEntry.transaction.value!!.toHexString()}","data":"${txEntry.transaction.input.toHexString("0x")}","chainId":${txEntry.transaction.chain}
                            }
                            """)
                    }
                } else {
                    rlp_image.visibility = View.GONE
                    rlp_header.visibility = View.GONE
                }

                if (transaction.isTokenTransfer()) {

                    GlobalScope.launch(Dispatchers.Main) {
                        val token = withContext(Dispatchers.Default) {
                            transaction.to?.let { appDatabase.tokens.forAddress(it) }
                        }
                        if (token != null) {
                            amountViewModel.setValue(transaction.getTokenTransferValue(), token)
                        } else {
                            amountViewModel.setValue(null, null)
                        }
                    }
                } else {
                    amountViewModel.setValue(transaction.value, getRootTokenForChain(networkDefinitionProvider.getCurrent()))
                }
                var message = "Hash:" + transaction.txHash
                txEntry.transactionState.error?.let { error ->
                    message += "\nError:$error"
                }
                details.text = message

                transaction.input.let {
                    GlobalScope.launch(Dispatchers.Main) {
                        val signatures = if (it.size >= 4) {
                            withContext(Dispatchers.Default) {
                                fourByteDirectory.getSignaturesFor(it.subList(0, 4).toHexString())
                            }
                        } else null

                        val hasFunction = it.isNotEmpty()

                        function_call_label.setVisibility(hasFunction)
                        function_call.setVisibility(hasFunction)

                        function_call.text = if (signatures?.isNotEmpty() == true) {
                            function_call_label.setText(R.string.function_call)
                            signatures.joinToString(
                                    separator = " ${getString(R.string.or)}\n",
                                    transform = { sig -> sig.textSignature ?: sig.hexSignature })
                        } else {
                            function_call_label.setText(R.string.function_data)
                            transaction.input.toHexString()
                        }
                    }
                }
            }
        })

    }

    override fun onCreateOptionsMenu(menu: Menu?) = super.onCreateOptionsMenu(menu.apply { menuInflater.inflate(R.menu.menu_transaction, menu) })

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_delete)?.isVisible = txEntity?.transactionState?.isPending ?: false
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_delete -> true.also {
            txEntity?.hash?.let {
                GlobalScope.async(Dispatchers.Main) {
                    withContext(Dispatchers.Default) {
                        appDatabase.transactions.deleteByHash(it)
                    }
                    finish()
                }
            }
        }

        R.id.menu_etherscan -> true.also {
            txEntity?.let {
                val url = blockExplorerProvider.get().getTransactionURL(it.transaction.txHash!!)
                startActivityFromURL(url)
            }
        }
        else -> super.onOptionsItemSelected(item)
    }
}
