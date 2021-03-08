package org.walleth.qr.scan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.walleth.base_activities.WallethActivity
import org.walleth.databinding.ActivityScanBinding

fun Activity.getQRScanActivity() = Intent(this, QRScanActivity::class.java)

open class QRScanActivity : WallethActivity() {

    private val binding by lazy { ActivityScanBinding.inflate(layoutInflater) }

    private val videographer: Videographer
        get() = Videographer(this).also {
            it.onSuccessfulScan = this::finishWithResult
        }

    private val cameraPermission: CameraPermission
        get() = CameraPermission(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()

        if (!cameraPermission.isGranted()) {
            cameraPermission.request()
        } else {
            videographer.open(binding.viewfinderView)
        }
    }

    override fun onPause() {
        super.onPause()

        if (videographer.isOpen) {
            videographer.close()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermission.handleRequestResult(requestCode, grantResults)
    }

    internal open fun finishWithResult(value: String) {
        val result = Intent()
        result.putExtra("SCAN_RESULT", value)
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}