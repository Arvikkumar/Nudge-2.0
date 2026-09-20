package com.example.gentlenudge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.isActive

/**
 * MinimalAnalogClock
 *
 * Precise native Jetpack Compose reproduction of the HTML 200x200 clock design:
 * - 12 tick marks (radius 64 to 72 units, stroke 0.6, #8E8E93 @ 0.40 opacity)
 * - Clockwise blue progress ring (radius 85 units, stroke 0.52, #3B82F6 @ 0.28 opacity)
 * - Hour hand (length 50 units, stroke 0.75, #8E8E93 @ 0.40 opacity)
 * - Minute hand (length 70 units, stroke 0.44, #8E8E93 @ 0.36 opacity)
 * - Second hand with counter-tail (tip 71 units, tail 11 units, stroke 0.27, #3B82F6 @ 0.34 opacity)
 * - Center pivot dot (radius 1.4 units, #3B82F6 @ 0.42 opacity)
 *
 * Real-time animation is driven by a lifecycle-aware frame loop isolated within this component.
 */
@Composable
fun MinimalAnalogClock(
    modifier: Modifier = Modifier
) {
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                currentTimeMillis = System.currentTimeMillis()
            }
        }
    }

    val calendar = remember { Calendar.getInstance() }

    Canvas(
        modifier = modifier.testTag("minimal_analog_clock")
    ) {
        calendar.timeInMillis = currentTimeMillis
        val ms = calendar.get(Calendar.MILLISECOND)
        val sec = calendar.get(Calendar.SECOND) + ms / 1000f
        val min = calendar.get(Calendar.MINUTE) + sec / 60f
        val hr = (calendar.get(Calendar.HOUR) % 12) + min / 60f

        val secAngle = sec * 6f
        val minAngle = min * 6f
        val hrAngle = hr * 30f

        val scale = size.width / 200f
        val centerOffset = Offset(size.width / 2f, size.height / 2f)

        // 1. Blue Progress Ring (Radius = 85 units, stroke = 1.4 units, #3B82F6 @ 0.54 opacity)
        val sweep = (min / 60f) * 360f
        if (sweep > 0.05f) {
            val ringDiameter = 170f * scale
            drawArc(
                color = Color(0xFF3B82F6).copy(alpha = 0.54f),
                startAngle = -90f,
                sweepAngle = sweep.coerceIn(0f, 360f),
                useCenter = false,
                topLeft = Offset(centerOffset.x - 85f * scale, centerOffset.y - 85f * scale),
                size = Size(ringDiameter, ringDiameter),
                style = Stroke(width = 1.4f * scale, cap = StrokeCap.Round)
            )
        }

        // 2. 12 Tick Marks (Outer = 72 units, Inner = 64 units, stroke = 1.3 units, #8E8E93 @ 0.57 opacity)
        val tickColor = Color(0xFF8E8E93).copy(alpha = 0.57f)
        val tickStrokeWidth = 1.3f * scale
        for (tickDeg in 0 until 360 step 30) {
            val rad = Math.toRadians((tickDeg - 90.0)).toFloat()
            val cosVal = cos(rad)
            val sinVal = sin(rad)
            val start = Offset(
                centerOffset.x + 64f * scale * cosVal,
                centerOffset.y + 64f * scale * sinVal
            )
            val end = Offset(
                centerOffset.x + 72f * scale * cosVal,
                centerOffset.y + 72f * scale * sinVal
            )
            drawLine(
                color = tickColor,
                start = start,
                end = end,
                strokeWidth = tickStrokeWidth,
                cap = StrokeCap.Round
            )
        }

        // 3. Hour Hand (Length = 50 units, stroke = 1.8 units, #8E8E93 @ 0.65 opacity)
        val hrRad = Math.toRadians((hrAngle - 90.0)).toFloat()
        val hrEnd = Offset(
            centerOffset.x + 50f * scale * cos(hrRad),
            centerOffset.y + 50f * scale * sin(hrRad)
        )
        drawLine(
            color = Color(0xFF8E8E93).copy(alpha = 0.65f),
            start = centerOffset,
            end = hrEnd,
            strokeWidth = 1.8f * scale,
            cap = StrokeCap.Round
        )

        // 4. Minute Hand (Length = 70 units, stroke = 1.2 units, #8E8E93 @ 0.60 opacity)
        val minRad = Math.toRadians((minAngle - 90.0)).toFloat()
        val minEnd = Offset(
            centerOffset.x + 70f * scale * cos(minRad),
            centerOffset.y + 70f * scale * sin(minRad)
        )
        drawLine(
            color = Color(0xFF8E8E93).copy(alpha = 0.60f),
            start = centerOffset,
            end = minEnd,
            strokeWidth = 1.2f * scale,
            cap = StrokeCap.Round
        )

        // 5. Second Hand with Counter-Tail (Tip = 71 units, Tail = 11 units, stroke = 0.85 units, #3B82F6 @ 0.65 opacity)
        val secRad = Math.toRadians((secAngle - 90.0)).toFloat()
        val secCos = cos(secRad)
        val secSin = sin(secRad)
        val secTail = Offset(
            centerOffset.x - 11f * scale * secCos,
            centerOffset.y - 11f * scale * secSin
        )
        val secTip = Offset(
            centerOffset.x + 71f * scale * secCos,
            centerOffset.y + 71f * scale * secSin
        )
        drawLine(
            color = Color(0xFF3B82F6).copy(alpha = 0.65f),
            start = secTail,
            end = secTip,
            strokeWidth = 0.85f * scale,
            cap = StrokeCap.Round
        )

        // 6. Center Pivot Dot (Radius = 2.2 units, #3B82F6 @ 0.70 opacity)
        drawCircle(
            color = Color(0xFF3B82F6).copy(alpha = 0.70f),
            radius = 2.2f * scale,
            center = centerOffset
        )
    }
}
