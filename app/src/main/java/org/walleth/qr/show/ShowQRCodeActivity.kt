package org.walleth.qr.show

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.databinding.ActivityFullscreenQrcodeBinding
import org.walleth.util.setQRCode
import java.lang.IllegalArgumentException

private const val KEY_CONTENT = "qrContent"
private const val KEY_ALTERNATE = "showAlternate"

fun Context.getQRCodeIntent(content: String,
                            showAlternateText: Boolean = false) = Intent(this, ShowQRCodeActivity::class.java).apply {
    putExtra(KEY_CONTENT, content)
    putExtra(KEY_ALTERNATE, showAlternateText)
}

class ShowQRCodeActivity : BaseSubActivity() {

    private val binding by lazy { ActivityFullscreenQrcodeBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.subtitle = getString(R.string.actionbar_subtitle_qr_code)
    }

    override fun onResume() {
        super.onResume()
        val content = intent.getStringExtra(KEY_CONTENT)?:throw(IllegalArgumentException("having no KEY_CONTENT in onResume()"))
        binding.fullscreenBarcode.setQRCode(content)

        if (intent.getBooleanExtra(KEY_ALTERNATE, false)) {
            binding.alternativeBarcodeText.text = content
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