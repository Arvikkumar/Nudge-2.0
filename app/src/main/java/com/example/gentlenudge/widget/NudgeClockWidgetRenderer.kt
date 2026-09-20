package com.example.gentlenudge.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * NudgeClockWidgetRenderer
 *
 * Precise native Android Canvas reproduction of the MinimalAnalogClock design:
 * - 200x200 virtual unit coordinate system mapped proportionally to widget size
 * - Blue Progress Ring (Radius 85 units, stroke 1.4 units, #3B82F6 @ 0.54 alpha)
 * - 12 Tick Marks (Outer 72 units, Inner 64 units, stroke 1.3 units, #8E8E93 @ 0.57 alpha)
 * - Hour Hand (Length 50 units, stroke 1.8 units, #8E8E93 @ 0.65 alpha)
 * - Minute Hand (Length 70 units, stroke 1.2 units, #8E8E93 @ 0.60 alpha)
 * - Second Hand with Counter-Tail (Tip 71 units, Tail 11 units, stroke 0.85 units, #3B82F6 @ 0.65 alpha)
 * - Center Pivot Dot (Radius 2.2 units, #3B82F6 @ 0.70 alpha)
 */
object NudgeClockWidgetRenderer {

    fun renderClockBitmap(
        widthPx: Int,
        heightPx: Int,
        calendar: Calendar = Calendar.getInstance()
    ): Bitmap {
        val size = minOf(widthPx, heightPx).coerceIn(120, 512)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap)

        drawClockOnCanvas(canvas, size.toFloat(), size.toFloat(), calendar)
        return bitmap
    }

    fun renderProgressRingBitmap(
        widthPx: Int = 512,
        heightPx: Int = 512,
        calendar: Calendar = Calendar.getInstance()
    ): Bitmap {
        val size = minOf(widthPx, heightPx).coerceIn(120, 512)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap)

        drawProgressRingOnCanvas(canvas, size.toFloat(), size.toFloat(), calendar)
        return bitmap
    }

    fun drawProgressRingOnCanvas(
        canvas: Canvas,
        width: Float,
        height: Float,
        calendar: Calendar
    ) {
        val sec = calendar.get(Calendar.SECOND).toFloat()
        val min = calendar.get(Calendar.MINUTE) + sec / 60f
        val sweep = (min / 60f) * 360f
        if (sweep <= 0.05f) return

        val effectiveDimension = minOf(width, height)
        val scale = effectiveDimension / 200f
        val cx = width / 2f
        val cy = height / 2f

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f * scale
            strokeCap = Paint.Cap.ROUND
            color = Color.argb((255 * 0.54f).roundToInt(), 0x3B, 0x82, 0xF6)
        }
        val ringRadius = 85f * scale
        val ringRect = RectF(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius)
        canvas.drawArc(ringRect, -90f, sweep.coerceIn(0f, 360f), false, ringPaint)
    }

    fun drawClockOnCanvas(
        canvas: Canvas,
        width: Float,
        height: Float,
        calendar: Calendar
    ) {
        val ms = calendar.get(Calendar.MILLISECOND)
        val sec = calendar.get(Calendar.SECOND) + ms / 1000f
        val min = calendar.get(Calendar.MINUTE) + sec / 60f
        val hr = (calendar.get(Calendar.HOUR) % 12) + min / 60f

        val secAngle = sec * 6f
        val minAngle = min * 6f
        val hrAngle = hr * 30f

        val effectiveDimension = minOf(width, height)
        val scale = effectiveDimension / 200f
        val cx = width / 2f
        val cy = height / 2f

        // 1. Blue Progress Ring (Radius = 85 units, stroke = 1.4 units, #3B82F6 @ 0.54 opacity)
        val sweep = (min / 60f) * 360f
        if (sweep > 0.05f) {
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.4f * scale
                strokeCap = Paint.Cap.ROUND
                color = Color.argb((255 * 0.54f).roundToInt(), 0x3B, 0x82, 0xF6)
            }
            val ringRadius = 85f * scale
            val ringRect = RectF(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius)
            canvas.drawArc(ringRect, -90f, sweep.coerceIn(0f, 360f), false, ringPaint)
        }

        // 2. 12 Tick Marks (Outer = 72 units, Inner = 64 units, stroke = 1.3 units, #8E8E93 @ 0.57 opacity)
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.3f * scale
            strokeCap = Paint.Cap.ROUND
            color = Color.argb((255 * 0.57f).roundToInt(), 0x8E, 0x8E, 0x93)
        }
        for (tickDeg in 0 until 360 step 30) {
            val rad = Math.toRadians((tickDeg - 90.0)).toFloat()
            val cosVal = cos(rad)
            val sinVal = sin(rad)
            val startX = cx + 64f * scale * cosVal
            val startY = cy + 64f * scale * sinVal
            val endX = cx + 72f * scale * cosVal
            val endY = cy + 72f * scale * sinVal
            canvas.drawLine(startX, startY, endX, endY, tickPaint)
        }

        // 3. Hour Hand (Length = 50 units, stroke = 1.8 units, #8E8E93 @ 0.65 opacity)
        val hrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.8f * scale
            strokeCap = Paint.Cap.ROUND
            color = Color.argb((255 * 0.65f).roundToInt(), 0x8E, 0x8E, 0x93)
        }
        val hrRad = Math.toRadians((hrAngle - 90.0)).toFloat()
        val hrEndX = cx + 50f * scale * cos(hrRad)
        val hrEndY = cy + 50f * scale * sin(hrRad)
        canvas.drawLine(cx, cy, hrEndX, hrEndY, hrPaint)

        // 4. Minute Hand (Length = 70 units, stroke = 1.2 units, #8E8E93 @ 0.60 opacity)
        val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * scale
            strokeCap = Paint.Cap.ROUND
            color = Color.argb((255 * 0.60f).roundToInt(), 0x8E, 0x8E, 0x93)
        }
        val minRad = Math.toRadians((minAngle - 90.0)).toFloat()
        val minEndX = cx + 70f * scale * cos(minRad)
        val minEndY = cy + 70f * scale * sin(minRad)
        canvas.drawLine(cx, cy, minEndX, minEndY, minPaint)

        // 5. Second Hand with Counter-Tail (Tip = 71 units, Tail = 11 units, stroke = 0.85 units, #3B82F6 @ 0.65 opacity)
        val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.85f * scale
            strokeCap = Paint.Cap.ROUND
            color = Color.argb((255 * 0.65f).roundToInt(), 0x3B, 0x82, 0xF6)
        }
        val secRad = Math.toRadians((secAngle - 90.0)).toFloat()
        val secCos = cos(secRad)
        val secSin = sin(secRad)
        val secTailX = cx - 11f * scale * secCos
        val secTailY = cy - 11f * scale * secSin
        val secTipX = cx + 71f * scale * secCos
        val secTipY = cy + 71f * scale * secSin
        canvas.drawLine(secTailX, secTailY, secTipX, secTipY, secPaint)

        // 6. Center Pivot Dot (Radius = 2.2 units, #3B82F6 @ 0.70 opacity)
        val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb((255 * 0.70f).roundToInt(), 0x3B, 0x82, 0xF6)
        }
        canvas.drawCircle(cx, cy, 2.2f * scale, pivotPaint)
    }
}
