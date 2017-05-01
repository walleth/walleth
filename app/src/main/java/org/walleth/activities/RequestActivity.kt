package org.walleth.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_request.*
import net.glxn.qrgen.android.QRCode
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.setVisibility
import org.walleth.App
import org.walleth.R
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.iac.toERC67String
import java.math.BigDecimal


class RequestActivity : AppCompatActivity() {

    lateinit var currentERC67String: String
    val keyStore: WallethKeyStore by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_request)

        supportActionBar?.subtitle = getString(R.string.request_transaction_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        refreshQR()

        request_hint.text = Html.fromHtml(getString(R.string.request_hint))
        request_hint.movementMethod = LinkMovementMethod()

        add_value_checkbox.setOnCheckedChangeListener { _, isChecked ->
            value_inputlayout.setVisibility(isChecked)
            refreshQR()
        }

        value_input_edittext.doAfterEdit {
            refreshQR()
        }
    }

    private fun refreshQR() {
        val relevantAddress = keyStore.getCurrentAddress()
        currentERC67String = relevantAddress.toERC67String()

        if (add_value_checkbox.isChecked) {
            try {
                currentERC67String = relevantAddress.toERC67String(BigDecimal(value_input_edittext.text.toString()))
            } catch (e: NumberFormatException) {
            }
        }

        receive_qrcode.setImageBitmap(QRCode.from(currentERC67String).bitmap())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_request, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_share -> {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, currentERC67String)
                type = "text/plain"
            }

            startActivity(sendIntent)
            true
        }
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
