package org.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_info.*
import org.walleth.R

class InfoActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_info)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        intro_text.text = Html.fromHtml(getString(R.string.info_text))
        intro_text.movementMethod = LinkMovementMethod()

    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
