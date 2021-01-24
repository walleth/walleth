package org.walleth.transactions

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_sign_transaction.*
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.walleth.R
import org.walleth.accounts.ACCOUNT_TYPE_MAP
import org.walleth.base_activities.BaseSubActivity
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.AppDatabase
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.addresses.getSpec
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.util.*

class SignTransactionActivity : BaseSubActivity() {

    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val appDatabase: AppDatabase by inject()
    private val chainInfoProvider: ChainInfoProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_sign_transaction)

        lifecycleScope.launch {
            val entry = appDatabase.addressBook.byAddress(currentAddressProvider.getCurrentNeverNull())
            val drawable = ACCOUNT_TYPE_MAP[entry.getSpec()?.type]?.actionDrawable
            fab.setImageResource(drawable ?: R.drawable.ic_action_done)
        }

        Transformations.switchMap(currentAddressProvider) { address ->
            appDatabase.transactions.getNonceForAddressLive(address, chainInfoProvider.getCurrent()!!.chainId)
        }.observe(this, Observer {

            if (nonce_input.text?.isBlank() != false) {
                val nonceBigInt = if (it != null && it.isNotEmpty()) {
                    it.maxOrNull()!! + ONE
                } else {
                    ZERO
                }
                nonce_input.setText(String.format(Locale.ENGLISH, "%d", nonceBigInt))
            }

        })

    }

    /*

    private fun refreshFee() {
        val fee = try {
            BigInteger(gas_price_input.text.toString()) * BigInteger(gas_limit_input.text.toString())
        } catch (numberFormatException: NumberFormatException) {
            ZERO
        }
        feeValueViewModel.setValue(fee, chainInfoProvider.getCurrent()?.getRootToken())
    }

        private fun createTransaction(): Transaction {

        val localERC681 = currentERC681

        val value = amountController.getValueOrZero()

        val txProto = if (currentTokenProvider.getCurrent().isRootToken()) createEmptyTransaction().copy(
                value = value,
                to = currentToAddress!!
        ) else ERC20TransactionGenerator(currentTokenProvider.getCurrent().address).transfer(currentToAddress!!, value).copy(
                value = ZERO
        )

        val transaction = txProto.copy(
                chain = chainInfoProvider.getCurrentChainId().value,
                from = currentAddressProvider.getCurrentNeverNull(),
                creationEpochSecond = currentTimeMillis() / 1000,
                nonce = nonce_input.asBigInitOrNull(),
                gasPrice = gas_price_input.asBigInitOrNull(),
                gasLimit = gas_limit_input.asBigInitOrNull()
        )

        if (currentTokenProvider.getCurrent().isRootToken() && localERC681.function != null) {
            transaction.input = localERC681.toTransactionInput()
        }

        return transaction
    }
        private fun calculateGasCost() = gas_price_input.asBigInteger() * gas_limit_input.asBigInteger()
    private fun hasEnoughETH() = amountController.getValueOrZero() + calculateGasCost() > currentBalanceSafely()

gas_station_image.setOnClickListener {
    startActivityFromURL("http://ethgasstation.info")
}


private fun estimateGas() {
    gas_limit_input.setText(DEFAULT_GAS_LIMIT_ETH_TX.toString())
    estimateGasLimit()
}

private fun estimateGasLimit(attempt: Int = 0) {
    lifecycleScope.launch {
        delay(attempt * attempt * 1000L) // exponential backoff
        if (currentToAddress != null) { // we at least need a to address to create a transaction
            val rpc = rpcProvider.get()

            try {
                val result = withContext(Dispatchers.Default) {
                    rpc?.estimateGas(createTransaction().copy(gasLimit = null)) ?: throw NullPointerException()
                }

                if (result != DEFAULT_GAS_LIMIT) {
                    gas_limit_input.setText(result.multiply(TWO).toString())
                } else {
                    gas_limit_input.setText(result.toString())
                }
                clearWarning(WARNING_GASESTIMATE)
            } catch (e: Exception) {
                var message = "You might want to set it manually."
                e.message?.let {
                    message = "This was the reason: $it\n$message"
                    if (e is EthereumRPCException) {
                        message += "code:${e.code}"
                    }
                }
                lifecycleScope.launch(Dispatchers.Main) {
                    if (e.message == "The execution failed due to an exception.") {
                        showWarning(WARNING_GASESTIMATE, "Executing this transaction to estimate the gas-limit failed due to an exception. You most likely should not execute this transaction on chain.")
                    } else {
                        showWarning(WARNING_GASESTIMATE, "Could not yet estimate gas-limit (attempt:  $attempt) - you might want to set it yourself - you can find it in the advanced options.")
                        estimateGasLimit(attempt + 1)
                    }
                }
            }
        }

    }
}



private var currentTxHash: String? = null

private fun prepareTransaction() {
    when (val type = currentAccount.getSpec()?.type) {
        ACCOUNT_TYPE_PIN_PROTECTED, ACCOUNT_TYPE_BURNER, ACCOUNT_TYPE_PASSWORD_PROTECTED -> getPasswordForAccountType(type) { pwd ->
            if (pwd != null) {
                startTransaction(pwd, createTransaction())
            }
        }
        ACCOUNT_TYPE_NFC -> startNFCSigningActivity(TransactionParcel(createTransaction()))
        ACCOUNT_TYPE_TREZOR -> signWithTrezorForResult.launch(getTrezorSignIntent(TransactionParcel(createTransaction())))
        ACCOUNT_TYPE_KEEPKEY -> signWithTrezorForResult.launch(getKeepKeySignIntent(TransactionParcel(createTransaction())))
    }
}


private val signWithTrezorForResult: ActivityResultLauncher<Intent> = registerForActivityResult(StartActivityForResult()) {
    if (it.resultCode == Activity.RESULT_OK) {
        currentTxHash = it.data?.getStringExtra("TXHASH")
        storeDefaultGasPriceAndFinish()
    }
}

private fun startTransaction(password: String?, transaction: Transaction) {
    lifecycleScope.launch(Dispatchers.Main) {

        fab_progress_bar.visibility = VISIBLE
        fab.isEnabled = false

        val error: String? = withContext(Dispatchers.Default) {
            try {
                val currentAddress = currentAddressProvider.getCurrentNeverNull()
                val signatureData = keyStore.getKeyForAddress(currentAddress, password ?: DEFAULT_PASSWORD)?.let {
                    Snackbar.make(fab, "Signing transaction", Snackbar.LENGTH_INDEFINITE).show()
                    transaction.signViaEIP155(it, chainInfoProvider.getCurrentChainId())
                }

                currentSignatureData = signatureData

                currentTxHash = transaction.encodeRLP(signatureData).keccak().toHexString()
                transaction.txHash = currentTxHash


                val entity = transaction.toEntity(signatureData = signatureData, transactionState = TransactionState())
                appDatabase.transactions.upsert(entity)
                null
            } catch (e: Exception) {
                e.message
            }
        }

        fab_progress_bar.visibility = INVISIBLE
        fab.isEnabled = true

        if (error != null) {
            alert("Could not sign transaction: $error")
        } else {
            storeDefaultGasPriceAndFinish()
        }
    }
}


private fun onFabClick() {
    if (to_address.text?.isEmpty() == true || currentToAddress == null) {

        currentShowCase = MaterialShowcaseView.Builder(this)
                .setTarget(address_list_button)
                .setDismissText(android.R.string.ok)
                .setContentText(R.string.create_tx_err)
                .setTargetTouchable(true)
                .setListener(object : IShowcaseListener {
                    override fun onShowcaseDismissed(showcaseView: MaterialShowcaseView?) {
                        processShowCaseViewState(false)
                    }

                    override fun onShowcaseDisplayed(showcaseView: MaterialShowcaseView?) {
                    }

                })
                .build()

        currentShowCase?.show(this)

        processShowCaseViewState(true)


    } else if (currentTokenProvider.getCurrent().isRootToken() && hasEnoughETH()) {
        alert(R.string.create_tx_error_not_enough_funds)
    } else if (!nonce_input.hasText()) {
        alert(title = R.string.nonce_invalid, message = R.string.please_enter_name)
    } else {
        if (currentTokenProvider.getCurrent().isRootToken() && currentERC681.function == null && amountController.getValueOrZero() == ZERO) {
            question(configurator = {
                setMessage(R.string.create_tx_zero_amount)
                setTitle(R.string.alert_problem_title)
            }, action = { prepareTransaction() })
        } else if (!currentTokenProvider.getCurrent().isRootToken() && amountController.getValueOrZero() > currentBalanceSafely()) {
            question(configurator = {
                setMessage(R.string.create_tx_negative_token_balance)
                setTitle(R.string.alert_problem_title)
            }, action = { prepareTransaction() })
        } else {
            prepareTransaction()
        }
    }
}

private fun storeDefaultGasPriceAndFinish() {
    val gasPrice = gas_price_input.asBigInteger()
    val chainId = chainInfoProvider.getCurrentChainId()
    if (gasPrice != settings.getGasPriceFor(chainId.value)) {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.default_gas_price, chainInfoProvider.getCurrent()!!.name))
                .setMessage(R.string.store_gas_price)
                .setPositiveButton(R.string.save) { _: DialogInterface, _: Int ->
                    settings.storeGasPriceFor(gasPrice, chainId.value)
                    finishAndFollowUp()
                }
                .setNegativeButton(R.string.no) { _: DialogInterface, _: Int ->
                    finishAndFollowUp()
                }
                .show()
    } else {
        finishAndFollowUp()
    }
}

private fun finishAndFollowUp() {
    if (isParityFlow()) {
        currentSignatureData?.let {
            val hex = it.r.toHexStringZeroPadded(64, false) +
                    it.s.toHexStringZeroPadded(64, false) +
                    (it.v - valueOf(35) - (it.extractChainID() ?: ZERO) * valueOf(2)).toHexStringZeroPadded(2, false)
            val intent = Intent(this, ParitySignerQRActivity::class.java)
                    .putExtra("signatureHex", hex)
            startActivity(intent)
        }
    }
    setResult(RESULT_OK, Intent().apply { putExtra("TXHASH", currentTxHash) })
    finish()
}
*/
}