package org.walleth.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_logs.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.walleth.R
import org.walleth.data.config.Settings
import java.io.IOException

class DebugWallethActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()
    private val settings: Settings by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_logs)

        golog_switch.setOnCheckedChangeListener { _, _ ->
            displayLog()
        }

        /*

        TODO-GETHOPT

        val verbosityList = listOf("silent", "error", "warn", "info", "debug", "detail", "max")
        geth_verbosity_spinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, verbosityList)
        geth_verbosity_spinner.setSelection(settings.currentGoVerbosity)
        geth_verbosity_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                settings.currentGoVerbosity = position
                Geth.setVerbosity(position.toLong())
            }

        }
        */

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
