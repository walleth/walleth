package org.walleth.activities.qrscan

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import org.walleth.R

private const val REQUEST_CAMERA = 0

class CameraPermission(val activity: Activity) {

    fun handleRequestResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA) {
            if (!grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(activity, R.string.no_camera_permission, Toast.LENGTH_LONG).show()
                activity.finish()
            }
        }
    }

    private fun getPermission() = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
    fun isGranted() = getPermission() == PackageManager.PERMISSION_GRANTED

    fun request() {
        ActivityCompat.requestPermissions(activity,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA)
    }
}