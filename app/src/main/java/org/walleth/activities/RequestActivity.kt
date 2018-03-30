package org.walleth.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_request.*
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.ligi.compat.HtmlCompat
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.setVisibility
import org.walleth.R
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.networks.isNoTestNet
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.isETH
import org.walleth.functions.extractValueForToken
import org.walleth.functions.setQRCode
import org.walleth.util.copyToClipboard

class RequestActivity : AppCompatActivity() {

    private lateinit var currentERC67String: String
    private val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()
    private val currentTokenProvider: CurrentTokenProvider by LazyKodein(appKodein).instance()
    private val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_request)

        supportActionBar?.subtitle = getString(R.string.request_transaction_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        refreshQR()


        val initText = getString(if (networkDefinitionProvider.getCurrent().isNoTestNet()) {
            R.string.request_hint_no_test_net
        } else {
            R.string.request_hint_test_net
        })
        request_hint.text = HtmlCompat.fromHtml(initText)
        request_hint.movementMethod = LinkMovementMethod()

        add_value_checkbox.setOnCheckedChangeListener { _, isChecked ->
            value_input_layout.setVisibility(isChecked)
            refreshQR()
        }

        value_input_edittext.doAfterEdit {
            refreshQR()
        }

        receive_qrcode.setOnClickListener({
            startActivity(Intent(this, FullscreenQRCodeActivity::class.java).apply {
                putExtra(KEY_ERC681, currentERC67String)
            })
        })
    }

    private fun refreshQR() {

        val currentToken = currentTokenProvider.currentToken
        if (currentToken.isETH()) {

            val relevantAddress = currentAddressProvider.getCurrent()
            currentERC67String = ERC681(address = relevantAddress.hex).generateURL()

            if (add_value_checkbox.isChecked) {
                try {
                    currentERC67String = ERC681(address = relevantAddress.hex, value = value_input_edittext.text.toString().extractValueForToken(currentToken)).generateURL()
                } catch (e: NumberFormatException) {
                }
            }
        } else {
            val relevantAddress = currentToken.address.hex

            val userAddress = currentAddressProvider.getCurrent().hex
            val functionParams = mutableListOf("address" to userAddress)
            if (add_value_checkbox.isChecked) {
                try {
                    functionParams.add("uint256" to value_input_edittext.text.toString().extractValueForToken(currentToken).toString())
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
        R.id.menu_share -> {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, currentERC67String)
                type = "text/plain"
            }

            startActivity(sendIntent)
            true
        }
        R.id.menu_copy -> {
            copyToClipboard(currentERC67String, receive_qrcode)
            true
        }
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
