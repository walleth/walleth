package org.walleth.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_logs.*
import org.walleth.R
import java.io.IOException

class DebugWallethActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_logs)

        golog_switch.setOnCheckedChangeListener { _, _ ->
            displayLog()
        }

        golog_switch.isChecked = true

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    private fun displayLog() {
        try {
            Thread(Runnable {
                val process = Runtime.getRuntime().exec("logcat -d")
                val text = process.inputStream.reader().readText()
                val textToPrint = if (golog_switch.isChecked) {
                    text.lines().filter { it.contains("GoLog") }.joinToString("\n")
                } else {
                    text
                }

                runOnUiThread {
                    log_text.text = textToPrint
                }
            }).start()

        } catch (e: IOException) {
            log_text.text = e.message
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        R.id.menu_share -> {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, log_text.text)
                type = "text/plain"
            }

            startActivity(sendIntent)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_debug, menu)
        return super.onCreateOptionsMenu(menu)
    }


}
