package org.walleth.activities

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_create_transaction.*
import kotlinx.android.synthetic.main.value.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.erc681.isEthereumURLString
import org.kethereum.erc681.parseERC681
import org.kethereum.functions.createTokenTransferTransactionInput
import org.kethereum.functions.encodeRLP
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.Address
import org.kethereum.model.createTransactionWithDefaults
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.startActivityFromURL
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.qrscan.startScanActivityForResult
import org.walleth.activities.trezor.TREZOR_REQUEST_CODE
import org.walleth.activities.trezor.startTrezorActivity
import org.walleth.data.AppDatabase
import org.walleth.data.DEFAULT_GAS_LIMIT_ERC_20_TX
import org.walleth.data.DEFAULT_GAS_LIMIT_ETH_TX
import org.walleth.data.addressbook.getByAddressAsync
import org.walleth.data.addressbook.resolveNameAsync
import org.walleth.data.balances.Balance
import org.walleth.data.config.Settings
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.networks.getNetworkDefinitionByChainID
import org.walleth.data.tokens.*
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.functions.asBigDecimal
import org.walleth.functions.decimalsAsMultiplicator
import org.walleth.functions.decimalsInZeroes
import org.walleth.functions.toFullValueString
import org.walleth.kethereum.android.TransactionParcel
import org.walleth.khex.toHexString
import org.walleth.ui.asyncAwait
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.text.ParseException

const val TO_ADDRESS_REQUEST_CODE = 1
const val FROM_ADDRESS_REQUEST_CODE = 2
const val TOKEN_REQUEST_CODE = 3

class CreateTransactionActivity : AppCompatActivity() {

    private var currentERC67String: String? = null
    private var currentAmount: BigInteger? = null
    private var currentToAddress: Address? = null

    private val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()
    private val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()
    private val currentTokenProvider: CurrentTokenProvider by LazyKodein(appKodein).instance()
    private val appDatabase: AppDatabase by LazyKodein(appKodein).instance()
    private val settings: Settings by LazyKodein(appKodein).instance()
    private var currentBalance: Balance? = null
    private var lastWarningURI: String? = null
    private var currentBalanceLive: LiveData<Balance>? = null


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            TREZOR_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    storeDefaultGasPrice()
                }
            }
            FROM_ADDRESS_REQUEST_CODE -> {
                data?.let {
                    if (data.hasExtra("HEX")) {
                        setFromAddress(Address(data.getStringExtra("HEX")))
                    }
                }
            }
            TOKEN_REQUEST_CODE -> {
                setAmountFromETHString(amount_input.text.toString())
                onCurrentTokenChanged()
            }
            else -> data?.let {
                if (data.hasExtra("HEX")) {
                    setToFromURL(data.getStringExtra("HEX"), fromUser = true)
                } else if (data.hasExtra("SCAN_RESULT")) {
                    setToFromURL(data.getStringExtra("SCAN_RESULT"), fromUser = true)
                }
            }
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_transaction)

        currentERC67String = if (savedInstanceState != null && savedInstanceState.containsKey("ERC67")) {
            savedInstanceState.getString("ERC67")
        } else {
            intent.data?.toString()
        }

        if (savedInstanceState != null && savedInstanceState.containsKey("lastERC67")) {
            lastWarningURI = savedInstanceState.getString("lastERC67")
        }

        supportActionBar?.subtitle = getString(R.string.create_transaction_on_network_subtitle,networkDefinitionProvider.getCurrent().getNetworkName())
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onCurrentTokenChanged()

        currentAddressProvider.observe(this, Observer { address ->
            address?.let {
                appDatabase.addressBook.getByAddressAsync(address) {
                    from_address.text = it?.name
                    val isTrezorTransaction = it?.trezorDerivationPath != null
                    fab.setImageResource(if (isTrezorTransaction) R.drawable.trezor_icon_black else R.drawable.ic_action_done)
                    fab.setOnClickListener {
                        onFabClick(isTrezorTransaction)
                    }
                }
            }
        })

        current_token_symbol.setOnClickListener {
            startActivityForResult(Intent(this, SelectTokenActivity::class.java), TOKEN_REQUEST_CODE)
        }

        gas_price_input.setText(settings.getGasPriceFor(networkDefinitionProvider.getCurrent()).toString())

        sweep_button.setOnClickListener {
            val balance = currentBalanceSafely()
            val amountAfterFee = balance - gas_price_input.asBigInit() * gas_limit_input.asBigInit()
            if (amountAfterFee < ZERO) {
                alert(R.string.no_funds_after_fee)
            } else {
                amount_input.setText(amountAfterFee.toFullValueString(currentTokenProvider.currentToken))
            }
        }

        gas_limit_input.doAfterEdit {
            refreshFee()
        }

        gas_price_input.doAfterEdit {
            refreshFee()
        }

        gas_station_image.setOnClickListener {
            startActivityFromURL("http://ethgasstation.info")
        }

        show_advanced_button.setOnClickListener {
            show_advanced_button.visibility = View.GONE
            fee_label.visibility = View.VISIBLE
            fee_value_view.visibility = View.VISIBLE
            gas_price_input_container.visibility = View.VISIBLE
            gas_limit_input_container.visibility = View.VISIBLE
            nonce_title.visibility = View.VISIBLE
            nonce_input_container.visibility = View.VISIBLE
        }

        Transformations.switchMap(currentAddressProvider, { address ->
            appDatabase.transactions.getNonceForAddressLive(address, networkDefinitionProvider.getCurrent().chain)
        }).observe(this, Observer {
            nonce_input.setText(if (it != null && !it.isEmpty()) {
                it.max()!! + ONE
            } else {
                ZERO
            }.toString())
        })
        refreshFee()
        setToFromURL(currentERC67String, false)

        scan_button.setOnClickListener {
            startScanActivityForResult(this)
        }

        address_list_button.setOnClickListener {
            val intent = Intent(this@CreateTransactionActivity, AddressBookActivity::class.java)
            startActivityForResult(intent, TO_ADDRESS_REQUEST_CODE)
        }

        from_address_list_button.setOnClickListener {
            val intent = Intent(this@CreateTransactionActivity, AddressBookActivity::class.java)
            startActivityForResult(intent, FROM_ADDRESS_REQUEST_CODE)
        }

        amount_input.doAfterEdit {
            setAmountFromETHString(it.toString())
            amount_value.setValue(currentAmount ?: ZERO, currentTokenProvider.currentToken)
        }

    }

    private fun onCurrentTokenChanged() {
        val currentToken = currentTokenProvider.currentToken
        currentBalanceLive = Transformations.switchMap(currentAddressProvider, { address ->
            appDatabase.balances.getBalanceLive(address, currentToken.address, networkDefinitionProvider.getCurrent().chain)
        })
        currentBalanceLive!!.observe(this, Observer {
            currentBalance = it
        })

        amount_value.setValue(currentAmount ?: ZERO, currentToken)

        if (currentToken.isETH()) {
            gas_limit_input.setText(DEFAULT_GAS_LIMIT_ETH_TX.toString())
        } else {
            gas_limit_input.setText(DEFAULT_GAS_LIMIT_ERC_20_TX.toString())
        }
    }

    private fun onFabClick(isTrezorTransaction: Boolean) {
        if (to_address.text.isEmpty() || currentToAddress == null) {
            alert(R.string.create_tx_error_address_must_be_specified)
        } else if (currentAmount == null) {
            alert(R.string.create_tx_error_amount_must_be_specified)
        } else if (currentTokenProvider.currentToken.isETH() && currentAmount!! + gas_price_input.asBigInit() * gas_limit_input.asBigInit() > currentBalanceSafely()) {
            alert(R.string.create_tx_error_not_enough_funds)
        } else if (!currentTokenProvider.currentToken.isETH() && currentAmount!! > currentBalanceSafely()) {
            alert(R.string.create_tx_error_not_enough_funds)
        } else if (nonce_input.text.isBlank()) {
            alert(title = R.string.nonce_invalid, message = R.string.please_enter_name)
        } else {
            val transaction = (if (currentTokenProvider.currentToken.isETH()) createTransactionWithDefaults(
                    value = currentAmount!!,
                    to = currentToAddress!!,
                    from = currentAddressProvider.getCurrent()
            ) else createTransactionWithDefaults(
                    creationEpochSecond = System.currentTimeMillis() / 1000,
                    value = ZERO,
                    to = currentTokenProvider.currentToken.address,
                    from = currentAddressProvider.getCurrent(),
                    input = createTokenTransferTransactionInput(currentToAddress!!, currentAmount!!)
            )).copy(chain = networkDefinitionProvider.getCurrent().chain, creationEpochSecond = System.currentTimeMillis() / 1000)

            transaction.nonce = nonce_input.asBigInit()
            transaction.gasPrice = gas_price_input.asBigInit()
            transaction.gasLimit = gas_limit_input.asBigInit()
            transaction.txHash = transaction.encodeRLP().keccak().toHexString()

            when {

                isTrezorTransaction -> startTrezorActivity(TransactionParcel(transaction))
                else -> async(UI) {
                    async(CommonPool) {
                        appDatabase.transactions.upsert(transaction.toEntity(signatureData = null, transactionState = TransactionState()))
                    }.await()
                    storeDefaultGasPrice()
                }

            }
        }
    }

    private fun currentBalanceSafely() = currentBalance?.balance ?: ZERO

    private fun TextView.asBigInit() = BigInteger(text.toString())

    private fun refreshFee() {
        val fee = try {
            BigInteger(gas_price_input.text.toString()) * BigInteger(gas_limit_input.text.toString())
        } catch (numberFormatException: NumberFormatException) {
            ZERO
        }
        fee_value_view.setValue(fee, getEthTokenForChain(networkDefinitionProvider.getCurrent()))
    }

    private fun setAmountFromETHString(amount: String) {
        currentAmount = try {
            (amount.asBigDecimal() * BigDecimal("1" + currentTokenProvider.currentToken.decimalsInZeroes())).toBigInteger()
        } catch (e: ParseException) {
            ZERO
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("ERC67", currentERC67String)
        outState.putString("lastERC67", lastWarningURI)
        super.onSaveInstanceState(outState)
    }

    private fun setFromAddress(address: Address) {
        if (currentAddressProvider.value != address) {
            currentBalance = null
            from_address.text = address.hex
            fab.setImageResource(R.drawable.ic_action_done)
            fab.setOnClickListener {
                onFabClick(false)
            }
            currentAddressProvider.setCurrent(address)
        }
    }

    private fun setToFromURL(uri: String?, fromUser: Boolean) {
        if (uri != null) {

            currentERC67String = if (uri.startsWith("0x")) ERC681(address = uri).generateURL() else uri

            if (parseERC681(currentERC67String!!).valid) {
                val erc681 = parseERC681(currentERC67String!!)

                showWarningOnWrongNetwork(erc681)

                currentToAddress = erc681.getToAddress()?.apply {
                    to_address.text = this.hex
                    appDatabase.addressBook.resolveNameAsync(this) {
                        to_address.text = it
                    }
                }

                if (erc681.isTokenTransfer()) {
                    if (erc681.address != null) {
                        { appDatabase.tokens.forAddress(Address(erc681.address!!)) }.asyncAwait { token ->
                            if (token != null) {

                                if (token != currentTokenProvider.currentToken) {
                                    currentTokenProvider.currentToken = token
                                    currentBalanceLive!!.removeObservers(this)
                                    onCurrentTokenChanged()
                                }

                                amount_input.setText(BigDecimal(erc681.getValueForTokenTransfer()).divide(token.decimalsAsMultiplicator()).toPlainString())
                            } else {
                                alert(getString(R.string.add_token_manually, erc681.address), getString(R.string.unknown_token))
                            }
                        }
                    } else {
                        alert(getString(R.string.no_token_address), getString(R.string.unknown_token))
                    }
                } else {
                    erc681.value?.let {

                        if (!currentTokenProvider.currentToken.isETH()) {
                            currentTokenProvider.currentToken = getEthTokenForChain(networkDefinitionProvider.getCurrent())
                            currentBalanceLive!!.removeObservers(this)
                            onCurrentTokenChanged()
                        }

                        amount_input.setText(BigDecimal(it).divide(currentTokenProvider.currentToken.decimalsAsMultiplicator()).toPlainString())

                        // when called from onCreate() the afterEdit hook is not yet added
                        setAmountFromETHString(amount_input.text.toString())
                        amount_value.setValue(currentAmount ?: ZERO, currentTokenProvider.currentToken)
                    }
                }

                erc681.gas?.let {
                    show_advanced_button.callOnClick()
                    gas_limit_input.setText(it.toString())
                }

            } else {
                currentToAddress = null
                to_address.text = getString(R.string.no_address_selected)
                if (fromUser || lastWarningURI != uri) {
                    lastWarningURI = uri
                    if (uri.isEthereumURLString()) {
                        alert(getString(R.string.create_tx_error_invalid_erc67_msg, uri), getString(R.string.create_tx_error_invalid_erc67_title))
                    } else {
                        alert(getString(R.string.create_tx_error_invalid_address, uri))
                    }

                }
            }
        }
    }

    private fun showWarningOnWrongNetwork(erc681: ERC681): Boolean {
        if (erc681.chainId != null && erc681.chainId != networkDefinitionProvider.getCurrent().chain.id) {
            val chainForTransaction = getNetworkDefinitionByChainID(erc681.chainId!!)?.getNetworkName() ?: erc681.chainId
            val currentNetworkName = networkDefinitionProvider.getCurrent().getNetworkName()
            val message = getString(R.string.please_switch_network, currentNetworkName, chainForTransaction)
            alert(title = getString(R.string.wrong_network), message = message)
            return true
        }
        return false
    }

    private fun storeDefaultGasPrice() {
        val gasPrice = gas_price_input.asBigInit()
        val networkDefinition = networkDefinitionProvider.getCurrent()
        if (gasPrice != settings.getGasPriceFor(networkDefinition)) {
            AlertDialog.Builder(this)
                    .setTitle(getString(R.string.default_gas_price, networkDefinition.getNetworkName()))
                    .setMessage(R.string.store_gas_price)
                    .setPositiveButton(R.string.save) { _: DialogInterface, _: Int ->
                        settings.storeGasPriceFor(gasPrice, networkDefinition)
                        finish()
                    }
                    .setNegativeButton(R.string.no) { _: DialogInterface, _: Int ->
                        finish()
                    }
                    .show()
        } else {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> true.also {
            finish()
        }
        else -> super.onOptionsItemSelected(item)
    }
}
