package org.walleth.activities

import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.android.synthetic.main.dialog_add_reference.view.*
import org.kodein.di.generic.instance
import org.walleth.R
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.ui.FiatListAdapter

class SelectReferenceActivity : BaseSubActivity() {

    private val exchangeRateProvider: ExchangeRateProvider by instance()
    private val settings: Settings by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list)

        supportActionBar?.subtitle = getString(R.string.select_reference_activity_select_reference)

        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = FiatListAdapter(exchangeRateProvider, this, settings)

        fab.setOnClickListener {
            val inflater = LayoutInflater.from(this@SelectReferenceActivity)
            val layout = inflater.inflate(R.layout.dialog_add_reference, null, false)

            AlertDialog.Builder(this@SelectReferenceActivity)
                    .setTitle(getString(R.string.select_reference_activity_add_reference))
                    .setView(layout)

                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        exchangeRateProvider.addFiat(layout.reference_text.text.toString().toUpperCase())
                        Handler().postDelayed({
                            recycler_view.adapter = FiatListAdapter(exchangeRateProvider, this, settings)
                        }, 1000)
                    }
                    .show()
        }
    }
}

