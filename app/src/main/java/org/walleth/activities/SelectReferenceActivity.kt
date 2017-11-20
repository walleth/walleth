package org.walleth.activities

import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.android.synthetic.main.dialog_add_reference.view.*
import org.walleth.R
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.ui.FiatListAdapter

class SelectReferenceActivity : AppCompatActivity() {

    private val exchangeRateProvider: ExchangeRateProvider by LazyKodein(appKodein).instance()
    private val settings: Settings by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list)

        supportActionBar?.subtitle = getString(R.string.select_reference_activity_select_reference)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = FiatListAdapter(exchangeRateProvider, this, settings)

        fab.setOnClickListener {
            val inflater = LayoutInflater.from(this@SelectReferenceActivity)
            val layout = inflater.inflate(R.layout.dialog_add_reference, null, false)

            AlertDialog.Builder(this@SelectReferenceActivity)
                    .setTitle(getString(R.string.select_reference_activity_add_reference))
                    .setView(layout)

                    .setPositiveButton(android.R.string.ok, { _, _ ->
                        exchangeRateProvider.addFiat(layout.reference_text.text.toString().toUpperCase())
                        Handler().postDelayed({
                            recycler_view.adapter = FiatListAdapter(exchangeRateProvider, this, settings)
                        }, 1000)
                    })
                    .show()
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

