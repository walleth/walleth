package org.ligi.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_request.*
import net.glxn.qrgen.android.QRCode
import org.ligi.walleth.App
import org.ligi.walleth.R
import org.ligi.walleth.iac.toERC67String

class RequestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_request)

        supportActionBar?.subtitle = getString(R.string.request_transaction_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val relevantAddress = App.keyStore.accounts[0].address
        receive_qrcode.setImageBitmap(QRCode.from(relevantAddress.toERC67String()).bitmap())

    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
