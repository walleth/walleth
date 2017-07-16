package org.walleth.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_view_transaction.*
import net.glxn.qrgen.android.QRCode
import org.kethereum.functions.encodeRLP
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromURL
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.addressbook.AddressBook
import org.walleth.data.exchangerate.ETH_TOKEN
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.transactions.TransactionProvider
import org.walleth.functions.resolveNameFromAddressBook
import org.walleth.khex.toHexString


class ViewTransactionActivity : AppCompatActivity() {

    companion object {
        private val HASH_KEY = "TXHASH"
        fun Context.getTransactionActivityIntentForHash(hex: String)
                = Intent(this, ViewTransactionActivity::class.java).apply {
            putExtra(HASH_KEY, hex)
        }
    }

    val transactionProvider: TransactionProvider by LazyKodein(appKodein).instance()
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()
    val addressBook: AddressBook by LazyKodein(appKodein).instance()
    val transaction by lazy {
        transactionProvider.getTransactionForHash(intent.getStringExtra(HASH_KEY))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_view_transaction)
    }

    override fun onResume() {
        super.onResume()


        transaction?.let {
            supportActionBar?.subtitle = getString(R.string.transaction_subtitle)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            nonce.text = transaction!!.transaction.nonce.toString()
            event_log_textview.text = it.state.eventLog

            fab.setVisibility(it.state.needsSigningConfirmation)
            fab.setOnClickListener { _ ->
                it.state.needsSigningConfirmation = false
                transactionProvider.updateTransaction(it.transaction.txHash!!, it)
                finish()
            }

            fee_value_view.setValue(it.transaction.gasLimit * it.transaction.gasPrice, ETH_TOKEN)

            val relevant_address = if (it.transaction.from == keyStore.getCurrentAddress()) {
                from_to_title.setText(R.string.transaction_to_label)
                it.transaction.to
            } else {
                from_to_title.setText(R.string.transaction_from_label)
                it.transaction.from
            }

            relevant_address?.let { ensured_relevant_address ->
                val name = ensured_relevant_address.resolveNameFromAddressBook(addressBook)
                from_to.text = name

                add_address.setVisibility(name == ensured_relevant_address.hex)

                add_address.setOnClickListener {
                    startCreateAccountActivity(ensured_relevant_address.hex)
                }

                copy_address.setOnClickListener {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(getString(R.string.ethereum_address), ensured_relevant_address.hex)
                    clipboard.primaryClip = clip
                    Snackbar.make(fab, R.string.snackbar_after_address_copy, Snackbar.LENGTH_LONG).show()
                }
            }


            if (!it.state.relayedEtherscan && !it.state.relayedLightClient) {
                rlp_header.setText(if (it.transaction.signatureData != null) {
                    R.string.signed_rlp_header_text
                } else {
                    R.string.unsigned_rlp_header_text
                })
                rlp_image.setImageBitmap(QRCode.from(it.transaction.encodeRLP().toHexString()).bitmap())
            } else {
                rlp_image.visibility = View.GONE
                rlp_header.visibility = View.GONE
            }

            value_view.setValue(it.transaction.value, ETH_TOKEN)
        } ?: alert("transaction not found " + intent.getStringExtra(HASH_KEY))

    }

    override fun onCreateOptionsMenu(menu: Menu?)
            = super.onCreateOptionsMenu(menu.apply { menuInflater.inflate(R.menu.menu_transaction, menu) })

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_etherscan -> {
            val url = networkDefinitionProvider.currentDefinition.getBlockExplorer().getURLforTransaction(transaction!!.transaction.txHash!!)
            startActivityFromURL(url)
            true
        }
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
