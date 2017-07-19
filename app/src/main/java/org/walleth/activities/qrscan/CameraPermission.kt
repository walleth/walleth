package org.walleth.activities.qrscan

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import org.walleth.R

class CameraPermission(val activity: Activity) {
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