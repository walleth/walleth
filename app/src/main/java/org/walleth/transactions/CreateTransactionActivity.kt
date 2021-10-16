package org.walleth.transactions

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View.*
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_create_transaction.*
import kotlinx.android.synthetic.main.value.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.bouncycastle.math.ec.ECConstants.TWO
import org.kethereum.DEFAULT_GAS_LIMIT
import org.kethereum.eip137.model.ENSName
import org.kethereum.eip155.extractChainID
import org.kethereum.eip155.signViaEIP155
import org.kethereum.ens.ENS_ADDRESS_NOT_FOUND
import org.kethereum.ens.isPotentialENSDomain
import org.kethereum.erc55.hasValidERC55ChecksumOrNoChecksum
import org.kethereum.erc55.isValid
import org.kethereum.erc681.*
import org.kethereum.erc831.isEthereumURLString
import org.kethereum.extensions.maybeHexToBigInteger
import org.kethereum.extensions.startsWith
import org.kethereum.extensions.toHexStringZeroPadded
import org.kethereum.extensions.transactions.*
import org.kethereum.keccakshortcut.keccak
import org.kethereum.keystore.api.KeyStore
import org.kethereum.metadata.model.NoMatchingUserDocFound
import org.kethereum.metadata.model.ResolveErrorUserDocResult
import org.kethereum.metadata.model.ResolvedUserDocResult
import org.kethereum.metadata.model.UserDocResultContractNotFound
import org.kethereum.metadata.repo.model.MetaDataRepo
import org.kethereum.metadata.repo.model.MetaDataResolveResultOK
import org.kethereum.metadata.resolveFunctionUserDoc
import org.kethereum.methodsignatures.CachedOnlineMethodSignatureRepository
import org.kethereum.methodsignatures.model.TextMethodSignature
import org.kethereum.model.*
import org.kethereum.rpc.EthereumRPCException
import org.koin.android.ext.android.inject
import org.komputing.kethereum.erc20.ERC20TransactionGenerator
import org.komputing.khex.extensions.hexToByteArray
import org.komputing.khex.extensions.toHexString
import org.komputing.khex.model.HexString
import org.ligi.compat.HtmlCompat
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxt.startActivityFromURL
import org.ligi.kaxtui.alert
import org.walleth.BuildConfig
import org.walleth.R
import org.walleth.accounts.ACCOUNT_TYPE_MAP
import org.walleth.accounts.AccountPickActivity
import org.walleth.base_activities.BaseSubActivity
import org.walleth.chains.ChainInfoProvider
import org.walleth.chains.chainIDAlert
import org.walleth.data.*
import org.walleth.data.addresses.AddressBookEntry
import org.walleth.data.addresses.CurrentAddressProvider
import org.walleth.data.addresses.getSpec
import org.walleth.data.addresses.resolveNameWithFallback
import org.walleth.data.ens.ENSProvider
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.rpc.RPCProvider
import org.walleth.data.tokens.*
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.kethereum.android.TransactionParcel
import org.walleth.nfc.startNFCSigningActivity
import org.walleth.qr.scan.getQRScanActivity
import org.walleth.sign.ParitySignerQRActivity
import org.walleth.startup.StartupActivity
import org.walleth.tokens.SelectTokenActivity
import org.walleth.trezor.getKeepKeySignIntent
import org.walleth.trezor.getTrezorSignIntent
import org.walleth.util.hasText
import org.walleth.util.question
import org.walleth.util.security.getPasswordForAccountType
import org.walleth.valueview.ValueViewController
import uk.co.deanwild.materialshowcaseview.IShowcaseListener
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView
import java.lang.System.currentTimeMillis
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.*
import java.util.*

private const val WARNING_USERDOC = "USERDOCWARN"
private const val WARNING_GASESTIMATE = "GASWARN"

class CreateTransactionActivity : BaseSubActivity() {

    private var currentERC681: ERC681 = ERC681()
    private var currentToAddress: Address? = null

    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val chainInfoProvider: ChainInfoProvider by inject()
    private val currentTokenProvider: CurrentTokenProvider by inject()
    private val keyStore: KeyStore by inject()
    private val appDatabase: AppDatabase by inject()
    private val exchangeRateProvider: ExchangeRateProvider by inject()
    private val rpcProvider: RPCProvider by inject()
    private val ensProvider: ENSProvider by inject()
    private val metaDataRepo: MetaDataRepo by inject()
    private val fourByteDirectory: CachedOnlineMethodSignatureRepository by inject()

    private var lastWarningURI: String? = null
    private var currentSignatureData: SignatureData? = null
    private var currentTxHash: String? = null

    private var currentShowCase: MaterialShowcaseView? = null

    private var currentAccount: AddressBookEntry? = null

    private val ensMap = mutableMapOf<String, String>()
    private val warningMap = mutableMapOf<String, String>()

    private var dataString: String? = null

    private val changeTokenForResult = registerForActivityResult(StartActivityForResult()) {
        onCurrentTokenChanged()
    }

    private val addActionForResult = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            setFromURL(it.data?.getStringExtra(EXTRA_KEY_ERC681), false)
        }
    }

    private val signWithTrezorForResult: ActivityResultLauncher<Intent> = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            currentTxHash = it.data?.getStringExtra("TXHASH")
            storeDefaultGasPriceAndFinish()
        }
    }

    private val selectToAddressForResult: ActivityResultLauncher<Intent> = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            setFromURL(it.data?.getStringExtra(EXTRA_KEY_ADDRESS), fromUser = true)
        }
    }

    private val selectFromAddressForResult: ActivityResultLauncher<Intent> = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            lifecycleScope.launch(Dispatchers.Main) {
                it.data?.getStringExtra(EXTRA_KEY_ADDRESS)?.let { address_string -> currentAddressProvider.setCurrent(Address(address_string)) }
            }
        }
    }

    private val scanQRForResult: ActivityResultLauncher<Intent> = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            setFromURL(it.data?.getStringExtra("SCAN_RESULT"), fromUser = true)
        }
    }


    private val amountController by lazy {
        ValueViewController(amount_value, exchangeRateProvider, settings)
    }
    private val feeValueViewModel by lazy {
        ValueViewController(fee_value_view, exchangeRateProvider, settings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_transaction)

        if (currentAddressProvider.getCurrent() == null) {
            alert("Account needed to make a transaction") {
                startActivityFromClass(StartupActivity::class.java)
                finish()
            }
        } else {
            createAfterCheck(savedInstanceState)
        }
    }

    private suspend fun String.toERC681() = if (startsWith("0x")) ERC681(address = this, chainId = ChainId(chainInfoProvider.getCurrent().chainId)) else parseERC681(this)

    private fun isParityFlow() = intent.getBooleanExtra("parityFlow", false)

    private fun createAfterCheck(savedInstanceState: Bundle?) {
        lifecycleScope.launch(Dispatchers.Main) {
            currentERC681 = if (savedInstanceState != null && savedInstanceState.containsKey("ERC67")) {
                savedInstanceState.getString("ERC67")
            } else {
                intent.data?.toString()
            }?.toERC681() ?: ERC681()

            if (savedInstanceState != null && savedInstanceState.containsKey("lastERC67")) {
                lastWarningURI = savedInstanceState.getString("lastERC67")
            }


            chainInfoProvider.getFlow().onEach {
                supportActionBar?.subtitle = getString(R.string.create_transaction_on_chain_subtitle, it.name)
            }.launchIn(lifecycleScope)

            currentTokenProvider.getFlow().onEach {
                onCurrentTokenChanged()
            }.launchIn(lifecycleScope)

            val gasPriceFromStringExtra = intent.getStringExtra("gasPrice")
            val s = when {
                gasPriceFromStringExtra != null -> HexString(gasPriceFromStringExtra).maybeHexToBigInteger()
                currentERC681.gasPrice != null -> currentERC681.gasPrice
                else -> chainInfoProvider.getCurrent().chainId.let {
                    settings.getGasPriceFor(it)
                }
            } ?: ONE

            gas_price_input.setText((s.toBigDecimal() / GIGA).toString())

            currentAddressProvider.flow.onEach { address ->
                address?.let {
                    lifecycleScope.launch(Dispatchers.Main) {
                        val entry = appDatabase.addressBook.byAddress(address)
                        currentAccount = entry
                        from_address.text = entry?.name
                        val drawable = ACCOUNT_TYPE_MAP[entry.getSpec()?.type]?.actionDrawable

                        fab.setImageResource(drawable ?: R.drawable.ic_action_done)
                    }
                }
            }.launchIn(lifecycleScope)

            intent.getStringExtra("data")?.let {
                val data = HexString(it).hexToByteArray()

                if (data.toList().startsWith(prefix = tokenTransferSignature)) {
                    currentERC681.function = "transfer"

                    val tmpTX = Transaction().apply {
                        input = data
                    }

                    currentERC681.functionParams = listOf(
                            "address" to tmpTX.getTokenTransferTo().hex,
                            "uint256" to tmpTX.getTokenTransferValue().toString()
                    )
                }

                data_label.visibility = VISIBLE
                data_text.visibility = VISIBLE
                data_text.text = it
                dataString = it


                lifecycleScope.launch(Dispatchers.IO) {
                    val signatures: Iterable<TextMethodSignature> = fourByteDirectory.getSignaturesFor(createTransaction())

                    lifecycleScope.launch(Dispatchers.Main) {
                        val signaturesText = signatures.joinToString("\n") { signature ->
                            signature.normalizedSignature
                        }
                        if (signaturesText.isNotBlank()) {
                            action_text.setVisibility(true)
                            action_text.text = signaturesText
                            action_label.visibility = VISIBLE
                        }

                    }
                }

            }

            currentAddressProvider.flow.onEach { address ->
                if (address != null) {
                    val nonce = withContext(Dispatchers.Default) {
                        appDatabase.transactions.getNonceForAddress(address, chainInfoProvider.getCurrent().chainId)
                    }

                    if (nonce_input.text?.isBlank() != false) {
                        val nonceBigInt = if (nonce != null && nonce.isNotEmpty()) {
                            nonce.maxOrNull()!! + ONE
                        } else {
                            ZERO
                        }
                        nonce_input.setText(String.format(Locale.ENGLISH, "%d", nonceBigInt))
                    }
                }
            }.launchIn(lifecycleScope)

            refreshFee()
            setFromURL(currentERC681.generateURL(), false)
        }

        fab.setOnClickListener {
            onFabClick()
        }

        current_token_symbol.setOnClickListener {
            changeTokenForResult.launch(Intent(this, SelectTokenActivity::class.java))
        }

        sweep_button.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val balance = currentBalanceSafely()
                lifecycleScope.launch(Dispatchers.Main) {
                    if (currentTokenProvider.getCurrent().isRootToken()) {
                        val amountAfterFee = balance - calculateGasCost()
                        if (amountAfterFee < ZERO) {
                            alert(R.string.no_funds_after_fee)
                        } else {
                            amountController.setValue(amountAfterFee, currentTokenProvider.getCurrent())
                        }
                    } else {
                        amountController.setValue(balance, currentTokenProvider.getCurrent())
                    }
                }
            }
        }

        gas_limit_input.doAfterEdit {
            refreshFee()
        }

        gas_price_input.doAfterEdit {
            refreshFee()
        }

        gas_station_image.setOnClickListener {
            startActivityFromURL("https://etherscan.io/gastracker")
        }

        show_advanced_button.setOnClickListener {
            show_advanced_button.visibility = GONE
            fee_label.visibility = VISIBLE
            fee_value_view.visibility = VISIBLE
            gas_price_input_container.visibility = VISIBLE
            gas_limit_input_container.visibility = VISIBLE
            nonce_title.visibility = VISIBLE
            nonce_input_container.visibility = VISIBLE
        }

        from_address_enter_button.setOnClickListener {
            val editText = EditText(this)
            val container = FrameLayout(this)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(16.toDp(), 0, 16.toDp(), 0)
            editText.layoutParams = params
            editText.isSingleLine = true
            container.addView(editText)

            AlertDialog.Builder(this)
                    .setTitle("Enter HEX or ENS address")
                    .setView(container)
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.cancel()
                        val ensName = ENSName(editText.text.toString())
                        when {
                            ensName.isPotentialENSDomain() -> {

                                lifecycleScope.launch {

                                    val address = withContext(lifecycleScope.coroutineContext + Dispatchers.IO) {
                                        ensProvider.get()?.getAddress(ensName)
                                    }
                                    if (address == null || address == ENS_ADDRESS_NOT_FOUND) {
                                        alert("could not resolve ENS address for ${editText.text}")
                                    } else {
                                        ensMap[address.toString()] = editText.text.toString()
                                        setFromURL(address.toString(), fromUser = true)
                                    }
                                }
                            }
                            editText.text.startsWith("0x") -> {
                                val address = Address(editText.text.toString())
                                if (!address.isValid()) {
                                    alert("Address is not valid")
                                } else if (!address.hasValidERC55ChecksumOrNoChecksum()) {
                                    alert("Address has invalid ERC55 checksum")
                                } else {
                                    setFromURL(address.toString(), fromUser = true)
                                }
                            }
                            else -> alert("Please enter either an ENS address (ending with .eth) or a HEX address (starting with 0x)")
                        }

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }


        address_list_button.setOnClickListener {
            val intent = Intent(this@CreateTransactionActivity, AccountPickActivity::class.java)
            selectToAddressForResult.launch(intent)
        }

        from_address_list_button.setOnClickListener {
            val intent = Intent(this@CreateTransactionActivity, AccountPickActivity::class.java)
            selectFromAddressForResult.launch(intent)
        }
    }

    private fun setUserDoc(msg: String) {
        action_label.visibility = VISIBLE
        action_text.visibility = VISIBLE
        action_text.text = msg
    }

    private fun showWarning(key: String, msg: String) {
        warningMap[key] = msg
        refreshWarning()
    }

    private fun clearWarning(key: String) {
        warningMap.remove(key)
        refreshWarning()
    }

    private fun refreshWarning() {
        warning_label.setVisibility(warningMap.isNotEmpty())
        warnings_text.setVisibility(warningMap.isNotEmpty())
        warning_indicator.setVisibility(warningMap.isNotEmpty())
        warnings_text.movementMethod = LinkMovementMethod.getInstance()
        val content = warningMap.values.joinToString("<br/>") {
            if (warningMap.size > 1) "• $it" else it
        }
        warnings_text.text = HtmlCompat.fromHtml(content)
        warning_label.setText(if (warningMap.keys.size == 1) R.string.create_transaction_warning_label else R.string.create_transaction_warnings_label)
    }

    private fun onCurrentTokenChanged() {
        lifecycleScope.launch(Dispatchers.Main) {
            val currentToken = currentTokenProvider.getCurrent()
            amountController.setValue(amountController.getValueOrZero(), currentToken)

            estimateGas()

        }
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

            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val currentBalanceSafely = currentBalanceSafely()
            val hasEnoughETH = amountController.getValueOrZero() + calculateGasCost() > currentBalanceSafely
            lifecycleScope.launch(Dispatchers.Main) {
                if (currentTokenProvider.getCurrent().isRootToken() && hasEnoughETH) {
                    alert(R.string.create_tx_error_not_enough_funds)
                } else if (!nonce_input.hasText()) {
                    alert(title = R.string.nonce_invalid, message = R.string.please_enter_nonce)
                } else {
                    if (dataString != null) {
                        prepareTransaction()
                    } else if (currentTokenProvider.getCurrent().isRootToken() && currentERC681.function == null && amountController.getValueOrZero() == ZERO) {
                        question(configurator = {
                            setMessage(R.string.create_tx_zero_amount)
                            setTitle(R.string.alert_problem_title)
                        }, action = { prepareTransaction() })
                    } else if (!currentTokenProvider.getCurrent().isRootToken() && amountController.getValueOrZero() > currentBalanceSafely) {
                        question(configurator = {
                            setMessage(R.string.create_tx_negative_token_balance)
                            setTitle(R.string.alert_problem_title)
                        }, action = { prepareTransaction() })
                    } else {
                        prepareTransaction()
                    }
                }
            }
        }
    }

    private fun calculateGasCost() = getGasPrice() * gas_limit_input.asBigInteger()

    private fun processShowCaseViewState(isShowcaseViewShown: Boolean) {
        if (isShowcaseViewShown) fab.hide() else fab.show()
        show_advanced_button.isEnabled = !isShowcaseViewShown
        amountController.setEnabled(!isShowcaseViewShown)
    }

    private fun prepareTransaction() {
        lifecycleScope.launch(Dispatchers.Main) {
            val transaction = createTransaction()
            when (val type = currentAccount.getSpec()?.type) {
                ACCOUNT_TYPE_PIN_PROTECTED, ACCOUNT_TYPE_BURNER, ACCOUNT_TYPE_PASSWORD_PROTECTED -> getPasswordForAccountType(type) { pwd ->
                    if (pwd != null) {
                        startTransaction(pwd, transaction)
                    }
                }
                ACCOUNT_TYPE_NFC -> startNFCSigningActivity(TransactionParcel(transaction))
                ACCOUNT_TYPE_TREZOR -> signWithTrezorForResult.launch(getTrezorSignIntent(TransactionParcel(transaction)))
                ACCOUNT_TYPE_KEEPKEY -> signWithTrezorForResult.launch(getKeepKeySignIntent(TransactionParcel(transaction)))
                ACCOUNT_TYPE_WATCH_ONLY -> alert("You have no key for this account")
            }
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

                    currentTxHash = transaction.encode(signatureData).keccak().toHexString()
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

    private suspend fun createTransaction(): Transaction {

        val localERC681 = currentERC681

        val value = amountController.getValueOrZero()

        val txProto = if (dataString != null || currentTokenProvider.getCurrent().isRootToken()) createEmptyTransaction().copy(
                value = value,
                to = if (currentToAddress?.cleanHex?.isEmpty() == true) null else currentToAddress
        ) else ERC20TransactionGenerator(currentTokenProvider.getCurrent().address).transfer(currentToAddress!!, value).copy(
                value = ZERO
        )

        val transaction = txProto.copy(
                chain = chainInfoProvider.getCurrentChainId().value,
                from = currentAddressProvider.getCurrentNeverNull(),
                creationEpochSecond = currentTimeMillis() / 1000,
                nonce = nonce_input.asBigInitOrNull(),
                gasPrice = getGasPrice(),
                gasLimit = gas_limit_input.asBigInitOrNull()
        )

        val immutableDataString = dataString
        if (immutableDataString != null) {
            transaction.input = HexString(immutableDataString).hexToByteArray()
        } else if (currentTokenProvider.getCurrent().isRootToken() && localERC681.function != null) {
            transaction.input = localERC681.toTransactionInput()
        }

        return transaction
    }

    private suspend fun currentBalanceSafely() = currentAccount?.address?.let { currentAddress ->
        chainInfoProvider.getCurrent().chainId.let { chainId ->
            appDatabase.balances.getBalance(currentAddress, currentTokenProvider.getCurrent().address, chainId)?.balance
        }
    } ?: ZERO

    private fun TextView.asBigInitOrNull() = try {
        BigInteger(text.toString())
    } catch (e: java.lang.NumberFormatException) {
        null
    }

    private fun TextView.asBigInteger() = BigInteger(text.toString())

    private fun refreshFee() {
        val fee = try {
            getGasPrice() * gas_limit_input.asBigInteger()
        } catch (numberFormatException: NumberFormatException) {
            ZERO
        }
        lifecycleScope.launch(Dispatchers.Main) {
            feeValueViewModel.setValue(fee, chainInfoProvider.getCurrent().getRootToken())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("ERC67", currentERC681.generateURL())
        outState.putString("lastERC67", lastWarningURI)
        super.onSaveInstanceState(outState)
    }

    private fun setFromURL(uri: String?, fromUser: Boolean) {
        if (uri == null) return

        lifecycleScope.launch(Dispatchers.Main) {

            val localERC681 = uri.toERC681()
            currentERC681 = localERC681

            if (currentERC681.valid) {

                chainIDAlert(chainInfoProvider,
                        appDatabase,
                        localERC681.chainId,
                        continuationWithWrongChainId = {
                            finish()
                        },
                        continuationWithCorrectOrNullChainId = {
                            lifecycleScope.launch(Dispatchers.Main) {
                                val address = currentERC681.address?.let { Address(it) }
                                val functionVisibility = currentERC681.function != null && !currentERC681.isTokenTransfer()
                                function_label.setVisibility(functionVisibility)
                                function_text.setVisibility(functionVisibility)

                                if (address != null) {

                                    currentERC681.address?.let { address ->


                                        val metaDataForAddressOnChain = withContext(Dispatchers.Default) {
                                            metaDataRepo.getMetaDataForAddressOnChain(Address(address), chainInfoProvider.getCurrentChainId())
                                        }

                                        action_button.setVisibility(metaDataForAddressOnChain is MetaDataResolveResultOK)

                                        if (metaDataForAddressOnChain is MetaDataResolveResultOK) {
                                            if (settings.isAdvancedFunctionsEnabled()) {
                                                from_contract_source_button.visibility = VISIBLE
                                                from_contract_source_button.setOnClickListener {
                                                    lifecycleScope.launch(Dispatchers.Main) {
                                                        val uri = Uri.parse("https://contractrepo.komputing.org/contract/${chainInfoProvider.getCurrent()?.chainId}/${currentERC681.address}/sources/")
                                                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                    }
                                                }
                                            }

                                            action_label.visibility = VISIBLE
                                            action_button.setText(if (currentERC681.function == null) R.string.add_action else R.string.change_action)
                                            action_text.visibility = GONE

                                            action_button.setOnClickListener {
                                                val noFunctionERC681 = currentERC681.copy(function = null, functionParams = emptyList())
                                                addActionForResult.launch(getERC681ActivityIntent(noFunctionERC681, ChangeActionActivity::class))
                                            }

                                        }
                                    }


                                    if (functionVisibility) {
                                        function_text.text = currentERC681.function + "(" + currentERC681.functionParams?.joinToString(",") { it.second } + ")"

                                        if (BuildConfig.FLAVOR_connectivity == "online") {
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                val res = currentERC681.resolveFunctionUserDoc(ChainId(chainInfoProvider.getCurrent()?.chainId!!), metaDataRepo)
                                                @Exhaustive
                                                when (res) {
                                                    is UserDocResultContractNotFound -> showWarning(WARNING_USERDOC, "Contact MetaData not found. Please <a href='wallethwarn:contractnotfound||" + currentERC681.address + "'>read here</a> to learn more.")
                                                    is ResolvedUserDocResult -> setUserDoc(res.userDoc)
                                                    is ResolveErrorUserDocResult -> showWarning(WARNING_USERDOC, "Cannot resolve Userdoc. " + res.error)
                                                    is NoMatchingUserDocFound -> showWarning(WARNING_USERDOC, "Userdoc for function not found. Please <a href='wallethwarn:userdocnotfound||" + currentERC681.address + "||" + currentERC681.function + "'>read here</a> to learn more.")
                                                }
                                            }
                                        }
                                    }
                                }


                                intent.getStringExtra("nonce")?.let {
                                    nonce_input.setText(HexString(it).maybeHexToBigInteger().toString())
                                }

                                currentToAddress = localERC681.getToAddress()?.apply {
                                    if (cleanHex.isEmpty()) {
                                        action_label.setVisibility(true)
                                        action_text.setVisibility(true)
                                        action_text.text = "Contract creation"
                                        to_address.setVisibility(false)
                                        to_label.setVisibility(false)
                                        from_address_enter_button.setVisibility(false)
                                        address_list_button.setVisibility(false)
                                    }
                                    to_address.text = appDatabase.addressBook.resolveNameWithFallback(this, ensMap[hex]?.let {
                                        "$it($hex)"
                                    } ?: hex)

                                }


                                if (localERC681.isTokenTransfer()) {
                                    if (localERC681.address != null) {
                                        val token = appDatabase.tokens.forAddress(Address(localERC681.address!!))
                                        if (token != null) {

                                            if (token != currentTokenProvider.getCurrent()) {
                                                currentTokenProvider.setCurrent(token)
                                                onCurrentTokenChanged()
                                            }

                                            localERC681.getValueForTokenTransfer()?.let {
                                                amountController.setValue(it, token)
                                            }
                                        } else {
                                            alert(getString(R.string.add_token_manually, localERC681.address), getString(R.string.unknown_token))
                                        }

                                    } else {
                                        alert(getString(R.string.no_token_address), getString(R.string.unknown_token))
                                    }
                                } else {

                                    if (localERC681.function != null) {
                                        if (!checkFunctionParameters(localERC681)) {
                                            localERC681.function = null
                                        }
                                    }

                                    localERC681.value?.let {

                                        if (!currentTokenProvider.getCurrent().isRootToken()) {
                                            chainInfoProvider.getCurrent().getRootToken().let { token ->
                                                currentTokenProvider.setCurrent(token)
                                                onCurrentTokenChanged()
                                            }
                                        }

                                        amountController.setValue(it, currentTokenProvider.getCurrent())
                                    }
                                }

                                localERC681.gasPrice?.let {
                                    show_advanced_button.callOnClick()
                                    gas_limit_input.setText(it.toString())
                                }

                                estimateGas()
                            }
                        })


            } else {
                currentToAddress = null
                to_address.setText(R.string.no_address_selected)
                if (fromUser || lastWarningURI != uri) {
                    lastWarningURI = uri
                    if (uri.isEthereumURLString()) {
                        alert(getString(R.string.create_tx_error_invalid_url_msg, uri), getString(R.string.create_tx_error_invalid_url_title))
                    } else {
                        alert(getString(R.string.create_tx_error_invalid_address, uri))
                    }

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        amountController.refreshNonValues()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_create_tx, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_scan -> true.also { scanQRForResult.launch(getQRScanActivity()) }
        else -> super.onOptionsItemSelected(item)
    }

    private fun checkFunctionParameters(localERC681: ERC681): Boolean {
        var errorString = localERC681.findIllegalParamType()?.let {
            getString(R.string.warning_invalid_param_type, it.first, it.second)
        }

        errorString = errorString ?: localERC681.findIllegalParamValue()?.let {
            getString(R.string.warning_invalid_parameter_value, it.second, it.first)
        }

        errorString?.also {
            alert(it) { finish() }
        }

        return errorString == null
    }

    private fun storeDefaultGasPriceAndFinish() {
        lifecycleScope.launch(Dispatchers.Main) {
            val gasPrice = getGasPrice()
            val chainId = chainInfoProvider.getCurrentChainId()
            if (!listOf(valueOf(4L), valueOf(5L), valueOf(100L)).contains(chainId.value) && gasPrice != settings.getGasPriceFor(chainId.value)) {
                AlertDialog.Builder(this@CreateTransactionActivity)
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
    }

    private fun getGasPrice() = (BigDecimal(gas_price_input.text.toString()) * GIGA).toBigInteger()

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
}