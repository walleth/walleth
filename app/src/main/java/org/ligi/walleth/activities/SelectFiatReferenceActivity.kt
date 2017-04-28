package org.ligi.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_select_fiat.*
import org.ligi.walleth.R
import org.ligi.walleth.data.exchangerate.ExchangeRateProvider
import org.ligi.walleth.ui.FiatListAdapter

class SelectFiatReferenceActivity : AppCompatActivity() {

    val exchangeRateProvider: ExchangeRateProvider by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_select_fiat)

        supportActionBar?.subtitle = "Select fiat reference"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fiat_list_recycler.layoutManager = LinearLayoutManager(this)
        fiat_list_recycler.adapter = FiatListAdapter(exchangeRateProvider)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

}

