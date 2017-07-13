package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.android.synthetic.main.activity_scan.*
import org.walleth.R
import org.walleth.ui.ReticleView

val REQUEST_CODE = 0x00006983

fun startScanActivityForResult(activity: Activity, requestCode: Int = REQUEST_CODE) {
    val intent = Intent(activity, ScanActivity::class.java)
    activity.startActivityForResult(intent, requestCode)
}

class ScanActivity : AppCompatActivity() {
    private val videographer = Videographer(this).also {
        it.onSuccessfulScan = this::finishWithResult
    }

    private val cameraPermission = CameraPermission(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
    }

    override fun onResume() {
        super.onResume()

        if (!cameraPermission.isGranted()) {
            cameraPermission.request()
        } else {
            videographer.open(viewfinderView)
        }
    }

    override fun onPause() {
        super.onPause()

        if (videographer.isOpen) {
            videographer.close()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        cameraPermission.handleRequestResult(requestCode, grantResults)
    }

    private fun finishWithResult(value: String) {
        val result = Intent()
        result.putExtra("SCAN_RESULT", value)
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}

private class CameraPermission(val activity: Activity) {
    companion object {
        private const val REQUEST_CAMERA = 0
    }

    fun handleRequestResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA) {
            if (!grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(activity, R.string.no_camera_permission, Toast.LENGTH_LONG).show()
                activity.finish()
            }
        }
    }

    fun isGranted(): Boolean {
        val permissionCheck = ActivityCompat.checkSelfPermission(activity,
                android.Manifest.permission.CAMERA)
        return permissionCheck == PackageManager.PERMISSION_GRANTED
    }

    fun request() {
        ActivityCompat.requestPermissions(activity,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA)
    }
}

@Suppress("DEPRECATION")
private class Videographer(val activity: Activity) {
    private val scanner = Scanner(this)

    private lateinit var camera: Camera

    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler

    private var previewWidth = 0
    private var previewHeight = 0

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            surface?.let { openCamera(it) }
        }
    }

    lateinit var onSuccessfulScan: (result: String) -> Unit

    var isOpen = false

    companion object {
        /**
         * The id of the first back-facing camera. Defaults to the first camera if not found.
         */
        val defaultCameraId: Int by lazy {
            findDefaultCamera()
        }

        private fun findDefaultCamera(): Int {
            val info = Camera.CameraInfo()

            for (i in 0 until Camera.getNumberOfCameras()) {
                Camera.getCameraInfo(i, info)

                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    return i
                }
            }

            return 0
        }
    }

    fun open(view: TextureView) {
        startBackgroundThread()

        if (view.isAvailable) {
            openCamera(view.surfaceTexture)
        } else {
            view.surfaceTextureListener = surfaceTextureListener
        }
    }

    fun close() {
        stopBackgroundThread()
        closeCamera()
    }

    fun capture(callback: (ByteArray, Int, Int) -> Unit) {
        camera.setOneShotPreviewCallback { data, _ ->
            callback(data, previewWidth, previewHeight)
        }
    }

    private fun startBackgroundThread() {
        thread = HandlerThread("VideographerBackgroundThread")
        thread.start()
        handler = Handler(thread.looper)
    }

    private fun stopBackgroundThread() {
        thread.quit()
        thread.join()
    }

    private fun openCamera(surface: SurfaceTexture) {
        handler.post {
            camera = Camera.open(defaultCameraId)

            setDefaultParameters()
            setPreviewSize()
            setCameraDisplayOrientation()

            camera.setPreviewTexture(surface)
            camera.startPreview()

            isOpen = true

            scanner.scan()
        }
    }

    private fun closeCamera() {
        camera.stopPreview()
        camera.release()
        isOpen = false
    }

    private fun setDefaultParameters() {
        val parameters = camera.parameters

        // NV21 is guaranteed to be available.
        // @see android.hardware.Camera.Parameters.setPreviewFormat(int)
        parameters.previewFormat = ImageFormat.NV21

        parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE

        camera.parameters = parameters
    }

    private fun setPreviewSize() {
        val previewSize = camera.parameters.previewSize
        previewWidth = previewSize.width
        previewHeight = previewSize.height
    }

    /**
     * Sets the camera display orientation to be the same as the display's.
     *
     * @see android.hardware.Camera.setDisplayOrientation(int)
     */
    private fun setCameraDisplayOrientation() {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(defaultCameraId, info)

        val rotation = activity.windowManager.defaultDisplay.rotation

        var degrees = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        degrees = (info.orientation - degrees + 360) % 360

        camera.setDisplayOrientation(degrees)
    }
}

private class Scanner(val videographer: Videographer) {
    fun scan() {
        videographer.capture(this::decode)
    }

    private fun decode(data: ByteArray, width: Int, height: Int) {
        val centerX = width / 2
        val centerY = height / 2

        var size = Math.min(width, height)
        size = (size.toDouble() * ReticleView.FRAME_SCALE).toInt()

        val halfSize = size / 2

        val left = centerX - halfSize
        val top = centerY - halfSize

        val source = PlanarYUVLuminanceSource(data, width, height, left, top, size, size, false)
        val image = BinaryBitmap(HybridBinarizer(source))
        val reader = QRCodeReader()

        try {
            val result = reader.decode(image)
            videographer.onSuccessfulScan(result.text)
        } catch (re: ReaderException) {
            scan()
        }
    }
}
