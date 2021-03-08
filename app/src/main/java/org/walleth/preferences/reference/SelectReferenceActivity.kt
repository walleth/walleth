package org.walleth.preferences.reference

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.android.ext.android.inject
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.databinding.ActivityListBinding
import org.walleth.databinding.DialogAddReferenceBinding

class SelectReferenceActivity : BaseSubActivity() {

    private val exchangeRateProvider: ExchangeRateProvider by inject()
    private val binding  by lazy { ActivityListBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list)

        supportActionBar?.subtitle = getString(R.string.select_reference_activity_select_reference)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = ReferenceListAdapter(exchangeRateProvider, this, settings)

        binding.fab.setOnClickListener {
            val inflater = LayoutInflater.from(this@SelectReferenceActivity)

            val layout = DialogAddReferenceBinding.inflate(inflater)

            AlertDialog.Builder(this@SelectReferenceActivity)
                    .setTitle(getString(R.string.select_reference_activity_add_reference))
                    .setView(layout.root)

                    .setPositiveButton(android.R.string.ok) { _, _ ->

                        val fiatName = layout.referenceText.text.toString().toUpperCase()

                        exchangeRateProvider.addFiat(fiatName)
                        Handler().postDelayed({
                            binding.recyclerView.adapter = ReferenceListAdapter(exchangeRateProvider, this, settings)
                        }, 1000)
                    }
                    .show()
        }
    }
}

