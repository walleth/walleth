package org.ligi.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_account_edit.*
import org.ligi.kaxt.startActivityFromURL
import org.ligi.walleth.App
import org.ligi.walleth.R

class EditAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_edit)

        supportActionBar?.subtitle = getString(R.string.edit_account_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        open_on_etherscan.setOnClickListener {
            startActivityFromURL(App.networḱ.getBlockExplorer().getURLforAddress(App.currentAddress!!))
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
