package org.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_list_simple.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.walleth.R
import org.walleth.ui.info.licenses.LicenseInfoAdapter
import org.walleth.ui.info.licenses.LicenseInfoEntry

class OpenSourceLicenseDisplayActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list_simple)

        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            subtitle = getString(R.string.activity_subtitle_oss_license)
        }

        val metadataText = readResource("third_party_license_metadata")
        val ins2 = metadataText.split("\n").filter { it.isNotBlank() }.map {
            it.split(" ").let {
                val positions = it.first().split(":")
                LicenseInfoEntry(it[1], positions.first().toInt(), positions.last().toInt())
            }
        }


        val data = readResource("third_party_licenses")

        recycler_view.adapter = LicenseInfoAdapter(ins2, data)
        recycler_view.layoutManager = LinearLayoutManager(this)
    }

    private fun readResource(name: String) = resources.openRawResource(getIdentifier(name)).reader().readText()

    private fun getIdentifier(name: String) = resources.getIdentifier(name, "raw", packageName)


    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> true.also {
            finish()
        }
        else -> super.onOptionsItemSelected(item)
    }
}
