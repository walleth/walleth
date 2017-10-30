package org.walleth.functions

import android.graphics.drawable.BitmapDrawable

import android.widget.ImageView
import net.glxn.qrgen.android.QRCode

fun ImageView.setQRCode(content: String) {
    val drawable = BitmapDrawable(resources, QRCode.from(content).bitmap())
    drawable.setAntiAlias(false)
    drawable.isFilterBitmap = false
    setImageDrawable(drawable)
}

