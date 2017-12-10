package org.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_info.*
import org.ligi.compat.HtmlCompat
import org.walleth.BuildConfig
import org.walleth.R

class InfoActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_info)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.subtitle = getString(R.string.info_activity_subtitle, BuildConfig.VERSION_NAME)

        intro_text.text = HtmlCompat.fromHtml(getString(R.string.info_text))
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
