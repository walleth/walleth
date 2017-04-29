package org.ligi.walleth.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_transfer.*
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxtui.alert
import org.ligi.walleth.R
import org.ligi.walleth.data.ETH_IN_WEI
import org.ligi.walleth.data.Transaction
import org.ligi.walleth.data.TransactionProvider
import org.ligi.walleth.data.keystore.WallethKeyStore
import org.ligi.walleth.iac.BarCodeIntentIntegrator
import org.ligi.walleth.iac.BarCodeIntentIntegrator.QR_CODE_TYPES
import org.ligi.walleth.iac.ERC67
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ZERO

class TransferActivity : AppCompatActivity() {

    var currentERC67String: String? = null
    var currentAmount: BigInteger? = null

    val transactionProvider: TransactionProvider by LazyKodein(appKodein).instance()
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()

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

        setContentView(R.layout.activity_transfer)

        currentERC67String = if (savedInstanceState != null && savedInstanceState.containsKey("ERC67")) {
            savedInstanceState.getString("ERC67")
        } else {
            intent.data?.toString()
        }

        supportActionBar?.subtitle = "Transfer"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setToFromURL(currentERC67String, false)

        scan_button.setOnClickListener {
            BarCodeIntentIntegrator(this).initiateScan(QR_CODE_TYPES)
        }

        address_list_button.setOnClickListener {
            val intent = Intent(this@TransferActivity, AddressBookActivity::class.java)
            startActivityForResult(intent, 23451)
        }

        amount_input.doAfterEdit {
            setAmountFromETHString(it.toString())
        }

        fab.setOnClickListener {
            if (currentERC67String == null) {
                alert("address must be specified")
            } else if (currentAmount == null) {
                alert("amount must be specified")
            } else {
                transactionProvider.addTransaction(Transaction(currentAmount!!, to = ERC67(currentERC67String!!).address, from = keyStore.getCurrentAddress()))
                finish()
            }
        }
    }

    private fun setAmountFromETHString(it: String) {
        currentAmount = try {
            (BigDecimal(it) * BigDecimal(ETH_IN_WEI)).toBigInteger()
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
            to_address.text = erc67.getHex()
            erc67.getValue()?.let {
                amount_input.setText((BigDecimal(it).setScale(4) / BigDecimal(ETH_IN_WEI)).toString())
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
