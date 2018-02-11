package org.walleth.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_fullscreen_qrcode.*
import org.walleth.R
import org.walleth.functions.setQRCode

const val KEY_ERC681 = "erc681"

class FullscreenQRCodeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_qrcode)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.subtitle = getString(R.string.request_transaction_subtitle)
    }

    override fun onResume() {
        super.onResume()
        val currentERC681 = intent.getStringExtra(KEY_ERC681)
        fullscreen_barcode.setQRCode(currentERC681)
        alternativeBarcodeText.text = currentERC681
        setToFullBrightness()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
            return true;
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    private fun setToFullBrightness() {
        val win = window
        val params = win.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        win.attributes = params
    }

}