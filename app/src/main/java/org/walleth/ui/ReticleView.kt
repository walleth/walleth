package org.walleth.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import org.walleth.R

class ReticleView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    companion object {
        const val FRAME_SCALE = 0.5
    }

    val framePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorPrimary)
        style = Paint.Style.STROKE
        strokeWidth = pxToDp(2.0f)
    }

    val marginPaint = Paint().apply {
        color = Color.argb(127, 0, 0, 0)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val frame = buildFrame(canvas.width, canvas.height)
        drawMargins(canvas, frame)
        drawFrame(canvas, frame)
    }

    private fun buildFrame(width: Int, height: Int): Rect {
        val centerX = width / 2
        val centerY = height / 2

        var size = if (height > width) { width } else { height }
        size = (size.toDouble() * FRAME_SCALE).toInt()

        val halfSize = size / 2

        val left = centerX - halfSize
        val top = centerY - halfSize
        val right = centerX + halfSize
        val bottom = centerY + halfSize

        return Rect(left, top, right, bottom)
    }

    private fun drawMargins(canvas: Canvas, frame: Rect) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        val left = frame.left.toFloat()
        val top = frame.top.toFloat()
        val right = frame.right.toFloat()
        val bottom = frame.bottom.toFloat()

        canvas.drawRect(0.0f, 0.0f, width, top, marginPaint)
        canvas.drawRect(0.0f, top, left, bottom, marginPaint)
        canvas.drawRect(right, top, width, bottom, marginPaint)
        canvas.drawRect(0.0f, bottom, width, height, marginPaint)
    }

    private fun drawFrame(canvas: Canvas, frame: Rect) {
        canvas.drawRect(frame, framePaint)
    }

    private fun pxToDp(value: Float): Float {
        val unit = TypedValue.COMPLEX_UNIT_DIP
        val metrics = resources.displayMetrics
        return TypedValue.applyDimension(unit, value, metrics)
    }
}
