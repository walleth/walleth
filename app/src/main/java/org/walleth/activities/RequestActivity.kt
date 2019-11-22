package org.walleth.activities

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_request.*
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.model.ChainId
import org.koin.android.ext.android.inject
import org.ligi.compat.HtmlCompat
import org.ligi.kaxt.setVisibility
import org.walleth.R
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.networks.ChainInfoProvider
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.getFaucetURL
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.isRootToken
import org.walleth.functions.setQRCode
import org.walleth.ui.valueview.ValueViewController
import org.walleth.util.copyToClipboard

class RequestActivity : BaseSubActivity() {

    private lateinit var currentERC67String: String
    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val currentTokenProvider: CurrentTokenProvider by inject()
    private val chainInfoProvider: ChainInfoProvider by inject()
    private val exchangeRateProvider: ExchangeRateProvider by inject()

    private var valueInputController: ValueViewController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_request)

        supportActionBar?.subtitle = getString(R.string.request_transaction_subtitle)

        valueInputController = object : ValueViewController(value_input, exchangeRateProvider, settings) {
            override fun refreshNonValues() {
                super.refreshNonValues()
                refreshQR()
            }
        }

        val initText = if (chainInfoProvider.getCurrent()?.faucets?.isNotEmpty() == true) {
            val faucetURL = chainInfoProvider.getCurrent()?.getFaucetURL(currentAddressProvider.getCurrentNeverNull())
            getString(R.string.request_faucet_message,
                    chainInfoProvider.getCurrent()!!.name,
                    faucetURL)
        } else {
            getString(R.string.no_faucet)
        }
        request_hint.text = HtmlCompat.fromHtml(initText)
        request_hint.movementMethod = LinkMovementMethod()

        add_value_checkbox.setOnCheckedChangeListener { _, isChecked ->
            value_input.setVisibility(isChecked)
            refreshQR()
        }

        receive_qrcode.setOnClickListener {
            startActivity(getQRCodeIntent(currentERC67String, showAlternateText = true))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshQR()
        valueInputController?.setValue(valueInputController?.getValueOrZero(), currentTokenProvider.getCurrent())
    }

    private fun refreshQR() {

        val currentToken = currentTokenProvider.getCurrent()
        if (!add_value_checkbox.isChecked || currentToken.isRootToken()) {

            val relevantAddress = currentAddressProvider.getCurrent()
            currentERC67String = ERC681(address = relevantAddress!!.hex).generateURL()

            if (add_value_checkbox.isChecked) {
                try {
                    currentERC67String = ERC681(
                            address = relevantAddress.hex,
                            value = valueInputController?.getValueOrZero(),
                            chainId = chainInfoProvider.getCurrent()?.let { ChainId(it.chainId) }
                    ).generateURL()
                } catch (e: NumberFormatException) {
                }
            }
        } else {
            val relevantAddress = currentToken.address.hex

            val userAddress = currentAddressProvider.getCurrentNeverNull().hex
            val functionParams = mutableListOf("address" to userAddress)
            if (add_value_checkbox.isChecked) {
                try {
                    functionParams.add("uint256" to valueInputController?.getValueOrZero().toString())
                } catch (e: NumberFormatException) {
                }
            }

            currentERC67String = ERC681(address = relevantAddress, function = "transfer",
                    functionParams = functionParams).generateURL()
        }

        receive_qrcode.setQRCode(currentERC67String)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_request, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_share -> true.also {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, currentERC67String)
                type = "text/plain"
            }

            startActivity(sendIntent)
        }
        R.id.menu_copy -> true.also {
            copyToClipboard(currentERC67String, receive_qrcode)
        }
        else -> super.onOptionsItemSelected(item)
    }
}
