package org.walleth.info

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_info.*
import org.ligi.compat.HtmlCompat
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity

const val SHARE_HINT = "\n\nPlease contact the contract authors (e.g. via the share icon on the top right) and let them know about the problem!"

class WarningActivity : BaseSubActivity() {

    private val payload by lazy { intent?.data.toString().substringAfter("wallethwarn:") }
    private val currentWarning by lazy { currentWarning() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_warning)

        supportActionBar?.setSubtitle(R.string.warning_subtitle)

        intro_text.text = HtmlCompat.fromHtml(currentWarning + SHARE_HINT)
        intro_text.movementMethod = LinkMovementMethod()

    }

    private fun currentWarning(): String {
        return when {
            payload.startsWith("userdocnotfound") -> getUserdocWarningText(payload)
            payload.startsWith("contractnotfound") -> getContractWarning(payload)
            else -> "Warning not found: $payload"
        }
    }

    private fun getUserdocWarningText(payload: String): String {
        val address = payload.split("||").getOrNull(1) ?: "unknown"
        val method = payload.split("||").getOrNull(2) ?: "unknown"
        return "The contract (at address $address) you tried to interact with did not contain any @notice on the method ($method) you tried to invoke. "
    }

    private fun getContractWarning(payload: String): String {
        val address = payload.split("||").getOrNull(1) ?: "unknown"
        return "The contract (at address $address) you tried to interact is not verified. Without knowing this it is dangerous to interact with this contract. It can be verified on https://verification.komputing.org"
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_warning, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_share -> true.also {
                startIntentWithCatch(Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, currentWarning)
                    type = "text/plain"
                })
            }

            R.id.menu_mail -> true.also {
                startIntentWithCatch(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "plain/text";
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("walleth@walleth.org"))
                    putExtra(Intent.EXTRA_SUBJECT, "[WallETH Warning]");
                    putExtra(Intent.EXTRA_TEXT, currentWarning);
                }, "Send mail"))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startIntentWithCatch(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            alert("Did not find any app to share with.")
        }
    }
}
