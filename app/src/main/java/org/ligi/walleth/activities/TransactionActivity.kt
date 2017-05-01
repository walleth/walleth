package org.ligi.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_transaction.*
import org.ligi.kaxt.startActivityFromURL
import org.ligi.walleth.R
import org.ligi.walleth.data.keystore.WallethKeyStore
import org.ligi.walleth.data.networks.NetworkDefinitionProvider
import org.ligi.walleth.data.transactions.TransactionProvider

class TransactionActivity : AppCompatActivity() {

    companion object {
        val HASH_KEY = "TXHASH"
    }

    val transactionProvider: TransactionProvider by LazyKodein(appKodein).instance()
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()
    val networkDefinitionProvider: NetworkDefinitionProvider by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_transaction)

        supportActionBar?.subtitle = getString(R.string.transaction_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val transaction = transactionProvider.getTransactionsForHash(intent.getStringExtra(HASH_KEY))!!
        tx_hash.text = transaction.txHash

        if (transaction.from == keyStore.getCurrentAddress()) {
            from_to_title.text = "To:"
            from_to.text = transaction.to.hex
        } else {
            from_to_title.text = "From:"
            from_to.text = transaction.from.hex
        }

        open_on_etherscan.setOnClickListener {
            val url = networkDefinitionProvider.networkDefinition.getBlockExplorer().getURLforTransaction(transaction.txHash!!)
            startActivityFromURL(url)
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
