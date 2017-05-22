package org.walleth.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_transaction.*
import org.ligi.kaxt.startActivityFromURL
import org.walleth.R
import org.walleth.data.addressbook.AddressBook
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
        transactionProvider.getTransactionForHash(intent.getStringExtra(HASH_KEY))!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_transaction)

        supportActionBar?.subtitle = getString(R.string.transaction_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        nonce.text = transaction.nonce.toString()
        fee_value_view.setEtherValue(transaction.gasLimit * transaction.gasPrice)

        if (transaction.from == keyStore.getCurrentAddress()) {
            from_to_title.setText(R.string.transaction_to_label)
            from_to.text = transaction.to.resolveNameFromAddressBook(addressBook)
        } else {
            from_to_title.setText(R.string.transaction_from_label)
            from_to.text = transaction.from.resolveNameFromAddressBook(addressBook)
        }

        value_view.setEtherValue(transaction.value)

    }

    override fun onCreateOptionsMenu(menu: Menu?)
            = super.onCreateOptionsMenu(menu.apply { menuInflater.inflate(R.menu.menu_transaction, menu) })

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_etherscan -> {
            val url = networkDefinitionProvider.networkDefinition.getBlockExplorer().getURLforTransaction(transaction.txHash!!)
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
