package org.walleth.activities.qrscan

import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.walleth.ui.ReticleView

class Scanner(private val videographer: Videographer) {
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