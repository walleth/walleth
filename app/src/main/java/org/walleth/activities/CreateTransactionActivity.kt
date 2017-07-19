package org.walleth.activities

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
import org.kethereum.functions.ERC67
import org.kethereum.functions.createTokenTransferTransactionInput
import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.startActivityFromURL
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.qrscan.startScanActivityForResult
import org.walleth.activities.trezor.startTrezorActivity
import org.walleth.data.BalanceProvider
import org.walleth.data.DEFAULT_GAS_LIMIT_ERC_20_TX
import org.walleth.data.DEFAULT_GAS_LIMIT_ETH_TX
import org.walleth.data.DEFAULT_GAS_PRICE
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.exchangerate.ETH_TOKEN
import org.walleth.data.exchangerate.TokenProvider
import org.walleth.data.exchangerate.isETH
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.transactions.TransactionProvider
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.TransactionWithState
import org.walleth.functions.decimalsInZeroes
import org.walleth.functions.resolveNameFromAddressBook
import org.walleth.kethereum.android.TransactionParcel
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO

class CreateTransactionActivity : AppCompatActivity() {

    var currentERC67String: String? = null
    var currentAmount: BigInteger? = null

    val transactionProvider: TransactionProvider by LazyKodein(appKodein).instance()
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    val addressBook: AddressBook by LazyKodein(appKodein).instance()
    val balanceProvider: BalanceProvider by LazyKodein(appKodein).instance()
    val tokenProvider: TokenProvider by LazyKodein(appKodein).instance()

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

        fab.setImageResource(if (isTrezorTransaction()) R.drawable.trezor_icon_black else R.drawable.ic_action_done)

        gas_price_input.setText(DEFAULT_GAS_PRICE.toString())

        if (tokenProvider.currentToken.isETH()) {
            gas_limit_input.setText(DEFAULT_GAS_LIMIT_ETH_TX.toString())
        } else {
            gas_limit_input.setText(DEFAULT_GAS_LIMIT_ERC_20_TX.toString())
        }

        sweep_button.setOnClickListener {
            val balance = balanceProvider.getBalanceForAddress(keyStore.getCurrentAddress(), tokenProvider.currentToken)!!.balance
            val amountAfterFee = balance - gas_price_input.asBigInit() * gas_limit_input.asBigInit()
            if (amountAfterFee < ZERO) {
                alert(R.string.no_funds_after_fee)
            } else {
                amount_input.setText(BigDecimal(amountAfterFee).divide(BigDecimal("1" + tokenProvider.currentToken.decimalsInZeroes())).toString())
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
        nonce_input.setText((transactionProvider.getLastNonceForAddress(keyStore.getCurrentAddress()) + ONE).toString())
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
            amount_value.setValue(currentAmount ?: ZERO, tokenProvider.currentToken)
        }

        amount_value.setValue(currentAmount ?: ZERO, tokenProvider.currentToken)

        fab.setOnClickListener {
            if (currentERC67String == null) {
                alert("address must be specified")
            } else if (currentAmount == null) {
                alert("amount must be specified")
            } else if (tokenProvider.currentToken == ETH_TOKEN && currentAmount!! + gas_price_input.asBigInit() * gas_limit_input.asBigInit() > balanceProvider.getBalanceForAddress(keyStore.getCurrentAddress(), tokenProvider.currentToken)!!.balance) {
                alert("Not enough funds for this transaction with the given amount plus fee")
            } else if (nonce_input.text.isBlank()) {
                alert(title = R.string.nonce_invalid, message = R.string.please_enter_name)
            } else {
                val transaction = if (tokenProvider.currentToken.isETH()) Transaction(
                        creationEpochSecond = System.currentTimeMillis() / 1000,
                        value = currentAmount!!,
                        to = ERC67(currentERC67String!!).address,
                        from = keyStore.getCurrentAddress()
                ) else Transaction(
                        creationEpochSecond = System.currentTimeMillis() / 1000,
                        value = ZERO,
                        to = Address(tokenProvider.currentToken.address),
                        from = keyStore.getCurrentAddress(),
                        input = createTokenTransferTransactionInput(ERC67(currentERC67String!!).address, currentAmount)
                )

                transaction.nonce = nonce_input.asBigInit()
                transaction.gasPrice = gas_price_input.asBigInit()
                transaction.gasLimit = gas_limit_input.asBigInit()

                when {
                    keyStore.hasKeyForForAddress(keyStore.getCurrentAddress()) -> {
                        transactionProvider.addPendingTransaction(TransactionWithState(transaction, TransactionState()))
                    }
                    isTrezorTransaction() -> startTrezorActivity(TransactionParcel(transaction))
                }
                finish()
            }
        }
    }

    private fun isTrezorTransaction() = addressBook.getEntryForName(keyStore.getCurrentAddress())?.trezorDerivationPath != null

    fun TextView.asBigInit() = BigInteger(text.toString())

    private fun refreshFee() {
        val fee = try {
            BigInteger(gas_price_input.text.toString()) * BigInteger(gas_limit_input.text.toString())
        } catch (numberFormatException: NumberFormatException) {
            ZERO
        }
        fee_value_view.setValue(fee, ETH_TOKEN)
    }

    private fun setAmountFromETHString(it: String) {
        currentAmount = try {
            (BigDecimal(it) * BigDecimal("1" + tokenProvider.currentToken.decimalsInZeroes())).toBigInteger()
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

            to_address.text = Address(erc67.getHex()).resolveNameFromAddressBook(addressBook)
            erc67.getValue()?.let {
                amount_input.setText((BigDecimal(it).setScale(4) / BigDecimal("1" + tokenProvider.currentToken.decimalsInZeroes())).toString())
                setAmountFromETHString(it)
                currentAmount = currentERC67String?.let { BigInteger(ERC67(it).getValue()) }
            }
        } else {
            to_address.text = "no address selected"

            if (fromUser) {
                alert("invalid address: \"$uri\" \n neither ERC67 nor plain hex")
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
