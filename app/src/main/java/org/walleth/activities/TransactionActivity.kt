package org.walleth.activities

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
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.NetworkDefinitionProvider
import org.walleth.data.transactions.TransactionProvider

class TransactionActivity : AppCompatActivity() {

    companion object {
        val HASH_KEY = "TXHASH"
    }

    val transactionProvider: TransactionProvider by LazyKodein(appKodein).instance()
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()
    val transaction by lazy { transactionProvider.getTransactionsForHash(intent.getStringExtra(HASH_KEY))!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_transaction)

        supportActionBar?.subtitle = getString(R.string.transaction_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tx_hash.text = transaction.txHash

        if (transaction.from == keyStore.getCurrentAddress()) {
            from_to_title.text = "To:"
            from_to.text = transaction.to.hex
        } else {
            from_to_title.text = "From:"
            from_to.text = transaction.from.hex
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
