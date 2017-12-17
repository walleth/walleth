package org.walleth.activities.qrscan

import android.app.Activity
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView

@Suppress("DEPRECATION")
class Videographer(val activity: Activity) {
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

    private fun openCamera(surface: SurfaceTexture)  {
        handler.post {
            try {
                camera = Camera.open(defaultCameraId)

                setDefaultParameters()
                setPreviewSize()
                setCameraDisplayOrientation()

                camera.setPreviewTexture(surface)
                camera.startPreview()

                isOpen = true

                scanner.scan()
            } catch (e: RuntimeException) {
                // https://github.com/walleth/walleth/issues/139
                e.printStackTrace()
            }
        }
    }

    private fun closeCamera() {
        if (::camera.isInitialized) {
            camera.stopPreview()
            camera.release()
            isOpen = false
        }
    }

    private fun setDefaultParameters() {
        val parameters = camera.parameters

        // NV21 is guaranteed to be available.
        // @see android.hardware.Camera.Parameters.getSupportedPreviewFormats()
        parameters.previewFormat = ImageFormat.NV21

        val focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        if (parameters.supportedFocusModes.contains(focusMode)) {
            parameters.focusMode = focusMode
        }

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
