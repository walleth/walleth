package org.walleth.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_transaction.*
import net.glxn.qrgen.android.QRCode
import org.kethereum.functions.toHexString
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

class TransactionActivity : AppCompatActivity() {

    companion object {
        private val HASH_KEY = "TXHASH"
        fun Context.getTransactionActivityIntentForHash(hex: String)
                = Intent(this, TransactionActivity::class.java).apply {
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

        setContentView(R.layout.activity_transaction)

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

            if (it.transaction.from == keyStore.getCurrentAddress()) {
                from_to_title.setText(R.string.transaction_to_label)
                from_to.text = it.transaction.to?.resolveNameFromAddressBook(addressBook)
            } else {
                from_to_title.setText(R.string.transaction_from_label)
                from_to.text = it.transaction.from.resolveNameFromAddressBook(addressBook)
            }

            if (it.transaction.signedRLP != null) {
                rlp_header.text = "Signed RLP"
                rlp_image.setImageBitmap(QRCode.from(it.transaction.signedRLP!!.toHexString()).bitmap())
            } else if (it.transaction.unSignedRLP != null) {
                rlp_header.text = "Unsigned RLP"
                rlp_image.setImageBitmap(QRCode.from(it.transaction.unSignedRLP!!.toHexString()).bitmap())
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
            val url = networkDefinitionProvider.networkDefinition.getBlockExplorer().getURLforTransaction(transaction!!.transaction.txHash!!)
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
