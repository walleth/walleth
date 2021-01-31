package org.walleth.transactions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_view_transaction.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.extensions.toHexString
import org.kethereum.extensions.transactions.encodeRLP
import org.kethereum.extensions.transactions.getTokenTransferTo
import org.kethereum.extensions.transactions.getTokenTransferValue
import org.kethereum.extensions.transactions.isTokenTransfer
import org.kethereum.methodsignatures.CachedOnlineMethodSignatureRepository
import org.koin.android.ext.android.inject
import org.komputing.khex.extensions.toHexString
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromURL
import org.walleth.R
import org.walleth.accounts.startCreateAccountActivity
import org.walleth.base_activities.BaseSubActivity
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.AppDatabase
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.addresses.resolveNameWithFallback
import org.walleth.data.blockexplorer.BlockExplorerProvider
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.tokens.getRootToken
import org.walleth.data.transactions.TransactionEntity
import org.walleth.qr.show.getQRCodeIntent
import org.walleth.util.setQRCode
import org.walleth.valueview.ValueViewController
import java.lang.IllegalStateException

private const val HASH_KEY = "TXHASH"
fun Context.getTransactionActivityIntentForHash(hex: String) = Intent(this, ViewTransactionActivity::class.java).apply {
    putExtra(HASH_KEY, hex)
}

class ViewTransactionActivity : BaseSubActivity() {

    private val appDatabase: AppDatabase by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val blockExplorerProvider: BlockExplorerProvider by inject()
    private val chainInfoProvider: ChainInfoProvider by inject()
    private val exchangeRateProvider: ExchangeRateProvider by inject()

    private var txEntity: TransactionEntity? = null
    private val fourByteDirectory: CachedOnlineMethodSignatureRepository by inject()

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

        appDatabase.transactions
                .getByHashLive(intent.getStringExtra(HASH_KEY)?:throw(IllegalStateException("no HASH_KEY string extra")))
                .observe(this, { txEntry ->
                    if (txEntry != null) {
                        txEntity = txEntry
                        invalidateOptionsMenu()
                        val transaction = txEntry.transaction

                        supportActionBar?.subtitle = getString(R.string.transaction_subtitle)

                        nonce.text = transaction.nonce.toString()
                        event_log_textview.text = txEntry.transactionState.eventLog

                        fab.setVisibility(txEntry.transactionState.needsSigningConfirmation)
                        fab.setOnClickListener {
                            lifecycleScope.launch(Dispatchers.Main) {
                                launch(Dispatchers.Default) {
                                    txEntry.transactionState.needsSigningConfirmation = false
                                    appDatabase.transactions.upsert(txEntry)
                                }

                                finish()
                            }
                        }

                        lifecycleScope.launch(Dispatchers.Default) {
                            val rootToken = appDatabase.chainInfo.getByChainId(txEntry.transaction.chain!!)?.getRootToken()
                            lifecycleScope.launch(Dispatchers.Main) {
                                feeViewModel.setValue(txEntry.transaction.gasLimit!! * txEntry.transaction.gasPrice!!, rootToken)
                            }
                        }
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

                        lifecycleScope.launch {
                            relevantAddress?.let { ensured_relevant_address ->
                                val name = appDatabase.addressBook.resolveNameWithFallback(ensured_relevant_address)
                                from_to.text = name
                                add_address.setVisibility(name == ensured_relevant_address.hex)


                                add_address.setOnClickListener {
                                    startCreateAccountActivity(ensured_relevant_address.hex)
                                }

                                copy_address.setOnClickListener {
                                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText(getString(R.string.ethereum_address), ensured_relevant_address.hex)
                                    clipboard.setPrimaryClip(clip)
                                    Snackbar.make(fab, R.string.snackbar_after_address_copy, Snackbar.LENGTH_LONG).show()
                                }
                            }
                        }

                        advanced_button.setOnClickListener {
                            advanced_container.visibility = View.VISIBLE
                            advanced_button.visibility = View.GONE
                        }


                        val rlpVisible = txEntry.transactionState.isPending && !txEntry.transactionState.needsSigningConfirmation && txEntry.transactionState.relayed.isEmpty()
                        rlp_container.setVisibility(rlpVisible)
                        if (rlpVisible) {

                            val content = if (txEntry.signatureData != null) {
                                rlp_header.setText(R.string.signed_rlp_header_text)
                                """{
                            "signedTransactionRLP":"${txEntry.transaction.encodeRLP(txEntry.signatureData).toHexString()}",
                            "chainId":${txEntry.transaction.chain}
                            }"""
                            } else {
                                rlp_header.setText(R.string.unsigned_rlp_header_text)
                                """{
"nonce":"${txEntry.transaction.nonce?.toHexString()}","gasPrice":"${txEntry.transaction.gasPrice!!.toHexString()}","gasLimit":"${txEntry.transaction.gasLimit!!.toHexString()}","to":"${txEntry.transaction.to}","from":"${txEntry.transaction.from}","value":"${txEntry.transaction.value!!.toHexString()}","data":"${txEntry.transaction.input.toHexString("0x")}","chainId":${txEntry.transaction.chain}
                            }
                            """
                            }
                            rlp_image.setQRCode(content)
                            rlp_copy_button.setOnClickListener {
                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.clipboard_copy_name), content))
                                Snackbar.make(fab, R.string.copied_to_clipboard, Snackbar.LENGTH_LONG).show()
                            }
                            rlp_share_button.setOnClickListener {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, content)
                                    type = "text/plain"
                                }
                                startActivity(sendIntent)
                            }
                            rlp_fullscreen_button.setOnClickListener {
                                startActivity(getQRCodeIntent(content))
                            }

                        }

                        if (transaction.isTokenTransfer()) {

                            lifecycleScope.launch(Dispatchers.Main) {
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
                            amountViewModel.setValue(transaction.value, chainInfoProvider.getCurrent()?.getRootToken())
                        }
                        var message = "Hash:" + transaction.txHash
                        txEntry.transactionState.error?.let { error ->
                            message += "\nError:$error"
                        }
                        details.text = message

                        lifecycleScope.launch(Dispatchers.Main) {
                            val signatures = withContext(Dispatchers.Default) {
                                fourByteDirectory.getSignaturesFor(transaction)
                            }.toList()
                            val hasFunction = transaction.input.size > 3

                            function_call_label.setVisibility(hasFunction)
                            function_call.setVisibility(hasFunction)

                            function_call.text = if (signatures.isNotEmpty()) {
                                function_call_label.setText(R.string.function_call)
                                signatures.joinToString(separator = " ${getString(R.string.or)}\n", transform = { it.signature })
                            } else {
                                function_call_label.setText(R.string.function_data)
                                transaction.input.toHexString()
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
                lifecycleScope.launch(Dispatchers.Main) {
                    withContext(Dispatchers.Default) {
                        appDatabase.transactions.deleteByHash(it)
                    }
                    finish()
                }
            }
        }

        R.id.menu_etherscan -> true.also {
            txEntity?.let {
                blockExplorerProvider.getOrAlert(this)?.run {
                    val url = getTransactionURL(it.transaction.txHash!!)
                    startActivityFromURL(url)
                }
            }
        }
        else -> super.onOptionsItemSelected(item)
    }
}
