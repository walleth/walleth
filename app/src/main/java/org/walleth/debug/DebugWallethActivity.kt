package org.walleth.debug

import android.content.Intent
import android.os.Bundle
import android.os.TransactionTooLargeException
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_logs.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import kotlin.math.max

class DebugWallethActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_logs)

        log_rpc_checkbox.isChecked = settings.logRPCRequests

        log_rpc_checkbox.setOnCheckedChangeListener { _, isChecked ->
            settings.logRPCRequests = isChecked
        }

        log_refresh_button.setOnClickListener {
            displayLog()
        }

        displayLog()

    }

    private fun displayLog() {
        lifecycleScope.launch(Dispatchers.Main) {
            var logString = ""
            try {


                withContext(Dispatchers.Default) {

                    val maxLines = Integer.parseInt(max_log_lines.text.toString())

                    readLogcatString().lines().reversed().forEachIndexed { index, s ->
                        if (index <= maxLines) {
                            logString += s + "\n"
                        }
                    }


                }
            } catch (e: Exception) {
                logString += e.toString()
            }

            log_text.text = logString
        }

    }

    private fun readLogcatString() = Runtime.getRuntime().exec("logcat -d").inputStream.reader().readText()

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_share -> true.also {
            try {
                sendLog(log_text.text)
            } catch (e: TransactionTooLargeException) {
                alert("Log too long - we need to shorten it") {
                    val textLength = log_text.text.length
                    val shortened = log_text.text.substring(max(log_text.text.length - 4096, 0), textLength)
                    sendLog(shortened)
                }
            }
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun sendLog(charSequence: CharSequence?) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, charSequence)
            type = "text/plain"
        }
        startActivity(sendIntent)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_debug, menu)
        return super.onCreateOptionsMenu(menu)
    }


}
