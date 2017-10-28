package org.walleth.activities

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_create_transaction.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import org.kethereum.erc67.ERC67
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
import org.walleth.activities.trezor.startTrezorActivity
import org.walleth.data.AppDatabase
import org.walleth.data.DEFAULT_GAS_LIMIT_ERC_20_TX
import org.walleth.data.DEFAULT_GAS_LIMIT_ETH_TX
import org.walleth.data.DEFAULT_GAS_PRICE
import org.walleth.data.addressbook.getByAddressAsync
import org.walleth.data.addressbook.resolveNameAsync
import org.walleth.data.balances.Balance
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.getEthTokenForChain
import org.walleth.data.tokens.isETH
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.functions.decimalsInZeroes
import org.walleth.kethereum.android.TransactionParcel
import org.walleth.khex.toHexString
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO

class CreateTransactionActivity : AppCompatActivity() {

    private var currentERC67String: String? = null
    private var currentAmount: BigInteger? = null

    private val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()
    private val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()
    private val currentTokenProvider: CurrentTokenProvider by LazyKodein(appKodein).instance()
    private val appDatabase: AppDatabase by LazyKodein(appKodein).instance()
    private var currentBalance: Balance? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        data?.let {
            if (data.hasExtra("HEX")) {
                setToFromURL(data.getStringExtra("HEX"), fromUser = true)
            } else if (data.hasExtra("SCAN_RESULT")) {
                setToFromURL(data.getStringExtra("SCAN_RESULT"), fromUser = true)
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

        supportActionBar?.subtitle = "Transfer"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        appDatabase.balances.getBalanceLive(currentAddressProvider.getCurrent(), currentTokenProvider.currentToken.address, networkDefinitionProvider.getCurrent().chain).observe(this, Observer {
            currentBalance = it
        })
        appDatabase.addressBook.getByAddressAsync(currentAddressProvider.getCurrent()) {
            val isTrezorTransaction = it?.trezorDerivationPath != null
            fab.setImageResource(if (isTrezorTransaction) R.drawable.trezor_icon_black else R.drawable.ic_action_done)

            fab.setOnClickListener {

                if (currentERC67String == null) {
                    alert("address must be specified")
                } else if (currentAmount == null) {
                    alert("amount must be specified")
                } else if (currentTokenProvider.currentToken.isETH() && currentAmount!! + gas_price_input.asBigInit() * gas_limit_input.asBigInit() > currentBalanceSafely()) {
                    alert("Not enough funds for this transaction with the given amount plus fee")
                } else if (nonce_input.text.isBlank()) {
                    alert(title = R.string.nonce_invalid, message = R.string.please_enter_name)
                } else {
                    val transaction = (if (currentTokenProvider.currentToken.isETH()) createTransactionWithDefaults(
                            value = currentAmount!!,
                            to = ERC67(currentERC67String!!).address,
                            from = currentAddressProvider.getCurrent()
                    ) else createTransactionWithDefaults(
                            creationEpochSecond = System.currentTimeMillis() / 1000,
                            value = ZERO,
                            to = currentTokenProvider.currentToken.address,
                            from = currentAddressProvider.getCurrent(),
                            input = createTokenTransferTransactionInput(ERC67(currentERC67String!!).address, currentAmount)
                    )).copy(chain = networkDefinitionProvider.getCurrent().chain, creationEpochSecond = System.currentTimeMillis() / 1000)

                    transaction.nonce = nonce_input.asBigInit()
                    transaction.gasPrice = gas_price_input.asBigInit()
                    transaction.gasLimit = gas_limit_input.asBigInit()
                    transaction.txHash = transaction.encodeRLP().keccak().toHexString()

                    when {

                        isTrezorTransaction -> startTrezorActivity(TransactionParcel(transaction))
                        else -> async(CommonPool) {
                            appDatabase.transactions.upsert(transaction.toEntity(signatureData = null, transactionState = TransactionState()))
                        }

                    }
                    finish()
                }
            }

        }



        gas_price_input.setText(DEFAULT_GAS_PRICE.toString())

        if (currentTokenProvider.currentToken.isETH()) {
            gas_limit_input.setText(DEFAULT_GAS_LIMIT_ETH_TX.toString())
        } else {
            gas_limit_input.setText(DEFAULT_GAS_LIMIT_ERC_20_TX.toString())
        }

        sweep_button.setOnClickListener {
            val balance = currentBalanceSafely()
            val amountAfterFee = balance - gas_price_input.asBigInit() * gas_limit_input.asBigInit()
            if (amountAfterFee < ZERO) {
                alert(R.string.no_funds_after_fee)
            } else {
                amount_input.setText(BigDecimal(amountAfterFee).divide(BigDecimal("1" + currentTokenProvider.currentToken.decimalsInZeroes())).toString())
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

        appDatabase.transactions.getNonceForAddressLive(currentAddressProvider.getCurrent(), networkDefinitionProvider.getCurrent().chain).observe(this, Observer {
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
            startActivityForResult(intent, 23451)
        }

        amount_input.doAfterEdit {
            setAmountFromETHString(it.toString())
            amount_value.setValue(currentAmount ?: ZERO, currentTokenProvider.currentToken)
        }

        amount_value.setValue(currentAmount ?: ZERO, currentTokenProvider.currentToken)


    }

    private fun currentBalanceSafely() = currentBalance?.balance ?: ZERO

    fun TextView.asBigInit() = BigInteger(text.toString())

    private fun refreshFee() {
        val fee = try {
            BigInteger(gas_price_input.text.toString()) * BigInteger(gas_limit_input.text.toString())
        } catch (numberFormatException: NumberFormatException) {
            ZERO
        }
        fee_value_view.setValue(fee, getEthTokenForChain(networkDefinitionProvider.getCurrent()))
    }

    private fun setAmountFromETHString(it: String) {
        currentAmount = try {
            (BigDecimal(it) * BigDecimal("1" + currentTokenProvider.currentToken.decimalsInZeroes())).toBigInteger()
        } catch (e: NumberFormatException) {
            ZERO
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("ERC67", currentERC67String)
        super.onSaveInstanceState(outState)
    }

    private fun setToFromURL(uri: String?, fromUser: Boolean) {
        uri?.let {
            currentERC67String = if (it.startsWith("0x")) "ethereum:$it" else uri
        }

        if (currentERC67String != null && ERC67(currentERC67String!!).isValid()) {
            val erc67 = ERC67(currentERC67String!!)

            appDatabase.addressBook.resolveNameAsync(Address(erc67.getHex())) {
                to_address.text = it
            }

            erc67.value?.let {
                amount_input.setText((BigDecimal(it).setScale(4) / BigDecimal("1" + currentTokenProvider.currentToken.decimalsInZeroes())).toString())
                setAmountFromETHString(it)
                currentAmount = currentERC67String?.let { BigInteger(ERC67(it).value) }
            }
        } else {
            to_address.text = "no address selected"

            if (fromUser) {
                alert("invalid address: \"$uri\" \n no or invalid ERC67 nor plain hex")
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
