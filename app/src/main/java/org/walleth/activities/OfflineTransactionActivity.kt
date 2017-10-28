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
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.kethereum.eip155.extractChainID
import org.kethereum.model.Address
import org.kethereum.model.ChainDefinition
import org.kethereum.model.SignatureData
import org.kethereum.model.createTransactionWithDefaults
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.ViewTransactionActivity.Companion.getTransactionActivityIntentForHash
import org.walleth.activities.qrscan.startScanActivityForResult
import org.walleth.data.AppDatabase
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.kethereum.geth.extractSignatureData
import org.walleth.kethereum.geth.toKethereumAddress
import org.walleth.khex.hexToByteArray
import java.math.BigInteger

class OfflineTransactionActivity : AppCompatActivity() {

    private val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()
    private val appDatabase: AppDatabase by LazyKodein(appKodein).instance()
    private val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_relay)

        supportActionBar?.subtitle = getString(R.string.relay_transaction)
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
                                createTransaction(gethTransaction, networkDefinitionProvider.getCurrent().chain.id,null, { currentAddressProvider.getCurrent() })
                            })
                            .show()
                } else {
                    val extractChainID = signatureData.extractChainID()
                    if (extractChainID == null) {
                        alert("Transaction is not EIP155 signed - cannot extract ChainID")
                    } else {
                        createTransaction(gethTransaction, extractChainID.toLong(), signatureData, {
                            val chainId = BigInt(extractChainID.toLong())
                            gethTransaction.getFrom(chainId).toKethereumAddress()
                        })
                    }
                }
            } catch (e: Exception) {
                alert(getString(R.string.input_not_valid_message, e.message), getString(R.string.input_not_valid_title))
            }
        }
    }

    private fun createTransaction(gethTransaction: org.ethereum.geth.Transaction, chainId: Long, signatureData: SignatureData?, from: () -> Address) {
        async(UI) {
            try {

                async(CommonPool) {
                    val transaction = createTransactionWithDefaults(
                            value = BigInteger(gethTransaction.value.toString()),
                            from = from.invoke(),
                            to = gethTransaction.to!!.toKethereumAddress(),
                            chain = ChainDefinition(4L),
                            nonce = BigInteger(gethTransaction.nonce.toString()),
                            txHash = gethTransaction.hash.hex
                    )
                    val transactionState = TransactionState(needsSigningConfirmation = signatureData == null)

                    appDatabase.transactions.upsert(transaction.toEntity(signatureData, transactionState))

                }.await()

                startActivity(getTransactionActivityIntentForHash(gethTransaction.hash.hex))
                finish()


            } catch (e: Exception) {
                alert("Problem " + e.message)
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
