package org.walleth.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.activity_relay.*
import org.ethereum.geth.Geth
import org.kethereum.functions.fromHexToByteArray
import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.ligi.kaxt.startActivityFromURL
import org.ligi.kaxtui.alert
import org.ligi.tracedroid.logging.Log
import org.walleth.R
import org.walleth.activities.TransactionActivity.Companion.getTransactionActivityIntentForHash
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.toKethereumAddress
import org.walleth.data.transactions.TransactionJSON
import org.walleth.data.transactions.TransactionProvider
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.TransactionWithState
import org.walleth.iac.BarCodeIntentIntegrator
import org.walleth.iac.BarCodeIntentIntegrator.QR_CODE_TYPES
import java.math.BigInteger

class OfflineTransactionActivity : AppCompatActivity() {

    val transactionProvider: TransactionProvider by LazyKodein(appKodein).instance()
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_relay)

        supportActionBar?.subtitle = "Relay transaction"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fab.setOnClickListener {
            try {
                val transactionRLP = fromHexToByteArray(transaction_to_relay_hex.text.toString())
                val gethTransaction = Geth.newTransactionFromRLP(transactionRLP)
                val json = gethTransaction.encodeJSON()
                val adapter = Moshi.Builder().build().adapter(TransactionJSON::class.java)
                val fromJson = adapter.fromJson(json)!!

                if (fromJson.r == "0x0" && fromJson.v == "0x0" && fromJson.s == "0x0") {
                    AlertDialog.Builder(this)
                            .setMessage("Unsigned transaction found - do you intend to sign with the current account? You will see the transaction details afterwards")
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok, { _, _ ->
                                createTransaction(gethTransaction, null, { keyStore.getCurrentAddress() })
                            })
                            .show()
                } else {
                    createTransaction(gethTransaction, transactionRLP.toList(), { gethTransaction.from.toKethereumAddress() })
                }
            } catch (e: Exception) {
                alert("Input not valid " + e.message)
            }
        }
    }

    private fun createTransaction(gethTransaction: org.ethereum.geth.Transaction, signedRLP: List<Byte>?, from: () -> Address) {
        try {
            val transaction = Transaction(
                    value = BigInteger(gethTransaction.value.toString()),
                    from = from.invoke(),
                    to = gethTransaction.to!!.toKethereumAddress(),

                    nonce = gethTransaction.nonce,
                    txHash = gethTransaction.hash.hex,
                    signedRLP = signedRLP
            )
            val transactionState = TransactionState(needsSigningConfirmation = signedRLP == null)
            transactionProvider.addTransaction(TransactionWithState(transaction, transactionState))

            Log.i("encodeJSON" + gethTransaction.encodeJSON())

            if (gethTransaction.sigHash == null) {
                alert("signed transaction")
            } else {
                startActivity(getTransactionActivityIntentForHash(gethTransaction.hash.hex))
                finish()
            }
        } catch (e: Exception) {
            if (e.message == "invalid transaction v, r, s values") {
                AlertDialog.Builder(this)
                        .setMessage("This seems to be a transaction from myEtherWallet - unfortunately this is not yet supported - a PR is pending click on details if you are interested in the issue.")
                        .setNegativeButton(android.R.string.ok, null)
                        .setNeutralButton("details", { _, _ ->
                            startActivityFromURL("https://github.com/ethereum/go-ethereum/issues/14599")
                        })
                        .show()
            } else {
                throw e
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_offline_transaction, menu)
        return super.onCreateOptionsMenu(menu)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {


        resultData?.let {
            if (it.hasExtra("SCAN_RESULT")) {
                transaction_to_relay_hex.setText(it.getStringExtra("SCAN_RESULT"))
            }
        }


    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.menu_scan -> {
            BarCodeIntentIntegrator(this).initiateScan(QR_CODE_TYPES)
            true
        }

        android.R.id.home -> {
            finish()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }
}
