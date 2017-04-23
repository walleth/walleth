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
import org.ligi.walleth.App
import org.ligi.walleth.R
import org.ligi.walleth.data.ETH_IN_WEI
import org.ligi.walleth.data.Transaction
import org.ligi.walleth.data.TransactionProvider
import org.ligi.walleth.data.toWallethAddress
import org.ligi.walleth.iac.BarCodeIntentIntegrator
import org.ligi.walleth.iac.BarCodeIntentIntegrator.QR_CODE_TYPES
import org.ligi.walleth.iac.ERC67
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ZERO

class TransferActivity : AppCompatActivity() {

    var currentERC67: ERC67? = null
    var currentAmount: BigInteger? = null

    val transactionProvider: TransactionProvider by LazyKodein(appKodein).instance()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && data.hasExtra("SCAN_RESULT")) {
            setToFromURL(data.getStringExtra("SCAN_RESULT"), fromUser = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_transfer)

        currentERC67 = savedInstanceState?.getString("ERC67")?.let(::ERC67)

        supportActionBar?.subtitle = "Transfer"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setToFromURL(intent.data?.toString(), false)

        scan_button.setOnClickListener {
            BarCodeIntentIntegrator(this).initiateScan(QR_CODE_TYPES)
        }

        amount_input.doAfterEdit {
            currentAmount = try {
                (BigDecimal(it.toString()) * BigDecimal(ETH_IN_WEI)).toBigInteger()
            } catch (e: NumberFormatException) {
                ZERO
            }

        }

        fab.setOnClickListener {
            if (currentERC67 == null) {
                alert("address must be specified")
            } else if (currentAmount == null) {
                alert("amount must be specified")
            } else {
                transactionProvider.addTransaction(Transaction(currentAmount!!, to = currentERC67!!.address, from = App.keyStore.accounts[0].address.toWallethAddress()))
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("ERC67", currentERC67?.url)
        super.onSaveInstanceState(outState)
    }

    private fun setToFromURL(uri: String?, fromUser: Boolean) {
        uri?.let {
            currentERC67 = ERC67(if (it.startsWith("0x")) "ethereum:$it" else uri)
        }

        if (currentERC67 != null && currentERC67!!.isValid()) {
            to_address.text = currentERC67!!.getHex()
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
