package org.walleth.activities

import android.os.Bundle
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_fullscreen_qrcode.*
import org.walleth.R
import org.walleth.functions.setQRCode

const val KEY_ERC681 = "erc681"

class QRCodeActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_qrcode)
        supportActionBar?.subtitle = getString(R.string.actionbar_subtitle_qr_code)
    }

    override fun onResume() {
        super.onResume()
        val currentERC681 = intent.getStringExtra(KEY_ERC681)
        fullscreen_barcode.setQRCode(currentERC681)
        alternativeBarcodeText.text = currentERC681
        setToFullBrightness()
    }

    private fun setToFullBrightness() {
        val win = window
        val params = win.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        win.attributes = params
    }

}