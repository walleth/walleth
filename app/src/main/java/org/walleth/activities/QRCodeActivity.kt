package org.walleth.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_fullscreen_qrcode.*
import org.walleth.R
import org.walleth.functions.setQRCode

private const val KEY_CONTENT = "qrContent"
private const val KEY_ALTERNATE = "showAlternate"

fun Context.getQRCodeIntent(content: String,
                            showAlternateText: Boolean = false) = Intent(this, QRCodeActivity::class.java).apply {
    putExtra(KEY_CONTENT, content)
    putExtra(KEY_ALTERNATE, showAlternateText)
}

class QRCodeActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_qrcode)
        supportActionBar?.subtitle = getString(R.string.actionbar_subtitle_qr_code)
    }

    override fun onResume() {
        super.onResume()
        val currentERC681 = intent.getStringExtra(KEY_CONTENT)
        fullscreen_barcode.setQRCode(currentERC681)

        if (intent.getBooleanExtra(KEY_ALTERNATE, false)) {
            alternativeBarcodeText.text = currentERC681
        }

        setToFullBrightness()
    }

    private fun setToFullBrightness() {
        val win = window
        val params = win.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        win.attributes = params
    }

}