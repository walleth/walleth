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
import kotlinx.android.synthetic.main.activity_relay.*
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.kethereum.model.Address
import org.kethereum.model.SignatureData
import org.kethereum.model.Transaction
import org.ligi.kaxt.startActivityFromURL
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.ViewTransactionActivity.Companion.getTransactionActivityIntentForHash
import org.walleth.activities.qrscan.startScanActivityForResult
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.transactions.TransactionProvider
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.TransactionWithState
import org.walleth.kethereum.geth.extractSignatureData
import org.walleth.kethereum.geth.toKethereumAddress
import org.walleth.khex.hexToByteArray
import java.math.BigInteger

class OfflineTransactionActivity : AppCompatActivity() {

    val transactionProvider: TransactionProvider by LazyKodein(appKodein).instance()
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_relay)

        supportActionBar?.subtitle = "Relay transaction"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fab.setOnClickListener {
            try {
                val transactionRLP = transaction_to_relay_hex.text.toString().hexToByteArray()
                val gethTransaction = Geth.newTransactionFromRLP(transactionRLP)
                val signatureData = gethTransaction.extractSignatureData()

                if (signatureData == null) {
                    AlertDialog.Builder(this)
                            .setMessage("Unsigned transaction found - do you intend to sign with the current account? You will see the transaction details afterwards")
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok, { _, _ ->
                                createTransaction(gethTransaction, null, { keyStore.getCurrentAddress() })
                            })
                            .show()
                } else {
                    createTransaction(gethTransaction, signatureData, {
                        val chainId = BigInt(networkDefinitionProvider.currentDefinition.chainId)
                        gethTransaction.getFrom(chainId).toKethereumAddress()
                    })
                }
            } catch (e: Exception) {
                alert("Input not valid " + e.message)
            }
        }
    }

    private fun createTransaction(gethTransaction: org.ethereum.geth.Transaction, signatureData: SignatureData?, from: () -> Address) {
        try {
            val transaction = Transaction(
                    value = BigInteger(gethTransaction.value.toString()),
                    from = from.invoke(),
                    to = gethTransaction.to!!.toKethereumAddress(),

                    nonce = BigInteger(gethTransaction.nonce.toString()),
                    txHash = gethTransaction.hash.hex,
                    signatureData = signatureData
            )
            val transactionState = TransactionState(needsSigningConfirmation = signatureData == null)
            transactionProvider.addTransaction(TransactionWithState(transaction, transactionState))

            startActivity(getTransactionActivityIntentForHash(gethTransaction.hash.hex))
            finish()

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
            startScanActivityForResult(this)
            true
        }

        android.R.id.home -> {
            finish()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }
}
