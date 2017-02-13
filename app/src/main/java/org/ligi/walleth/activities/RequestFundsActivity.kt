package org.ligi.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_receive_funds.*
import net.glxn.qrgen.android.QRCode
import org.ligi.walleth.App
import org.ligi.walleth.R
import org.ligi.walleth.toERC67String

class RequestFundsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_receive_funds)

        supportActionBar?.subtitle = getString(R.string.account_creation_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        receive_qrcode.setImageBitmap(QRCode.from(App.accountManager.accounts[0].address.toERC67String()).bitmap())
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
