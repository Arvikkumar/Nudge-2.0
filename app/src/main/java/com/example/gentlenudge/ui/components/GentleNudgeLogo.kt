package com.example.gentlenudge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the official Gentle Nudge brand mark:
 * An analog clock face with tick marks, hour & minute hands,
 * surrounded by an outer sweeping crescent arrow.
 */
@Composable
fun GentleNudgeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = Color(0xFF1C1A17)
) {
    Canvas(
        modifier = modifier.size(size)
    ) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w * 0.5f, h * 0.5f)
        val radius = w * 0.40f

        // 1. Draw outer crescent arrow
        // Sweeping arc from bottom right around left to top
        val crescentPath = Path().apply {
            // Outer arc from ~4:30 o'clock (45 deg below horizontal right = 45 deg) around through 180 to top (270 deg / -90 deg)
            // In Android Canvas: 0 is 3 o'clock, 90 is 6 o'clock, 180 is 9 o'clock, 270 is 12 o'clock
            val outerRadius = radius * 1.08f
            val innerRadius = radius * 0.88f

            // Start at bottom right tip (approx 35 deg)
            val startAngle = 35f
            val sweepAngle = 238f // sweeps from 35 clockwise to ~273 (top)

            // Outer arc
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    center.x - outerRadius,
                    center.y - outerRadius,
                    center.x + outerRadius,
                    center.y + outerRadius
                ),
                startAngleDegrees = startAngle,
                sweepAngleDegrees = sweepAngle,
                forceMoveTo = true
            )

            // Arrow head at the top (pointing right)
            val arrowX = center.x - 2f * (w / 100f)
            val arrowY = center.y - outerRadius - 3f * (h / 100f)
            val tipX = center.x + 10f * (w / 100f)
            val tipY = center.y - outerRadius + 2f * (h / 100f)
            val baseBottomX = center.x - 2f * (w / 100f)
            val baseBottomY = center.y - outerRadius + 8f * (h / 100f)

            lineTo(arrowX, arrowY)
            lineTo(tipX, tipY)
            lineTo(baseBottomX, baseBottomY)

            // Inner arc back to bottom right tip
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    center.x - innerRadius,
                    center.y - innerRadius,
                    center.x + innerRadius,
                    center.y + innerRadius
                ),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -235f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(path = crescentPath, color = tint, style = Fill)

        // 2. Draw 12 Hour Ticks
        for (i in 0 until 12) {
            val angleRad = (i * 30.0 - 90.0) * (PI / 180.0)
            val isCardinal = (i % 3 == 0) // 12, 3, 6, 9
            val tickLen = if (isCardinal) radius * 0.20f else radius * 0.12f
            val strokeW = if (isCardinal) w * 0.045f else w * 0.03f

            val outerPos = Offset(
                x = center.x + (radius * 0.78f) * cos(angleRad).toFloat(),
                y = center.y + (radius * 0.78f) * sin(angleRad).toFloat()
            )
            val innerPos = Offset(
                x = center.x + (radius * 0.78f - tickLen) * cos(angleRad).toFloat(),
                y = center.y + (radius * 0.78f - tickLen) * sin(angleRad).toFloat()
            )

            drawLine(
                color = tint,
                start = innerPos,
                end = outerPos,
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }

        // 3. Clock Hands
        // Center hub
        drawCircle(
            color = tint,
            radius = radius * 0.15f,
            center = center
        )

        // Hour Hand: pointing toward 10:15 (approx -135 deg / 225 deg)
        val hourAngle = (-135.0) * (PI / 180.0)
        val hourLength = radius * 0.44f
        drawLine(
            color = tint,
            start = center,
            end = Offset(
                x = center.x + hourLength * cos(hourAngle).toFloat(),
                y = center.y + hourLength * sin(hourAngle).toFloat()
            ),
            strokeWidth = w * 0.065f,
            cap = StrokeCap.Round
        )

        // Minute Hand: pointing horizontally to 3 o'clock (0 deg)
        val minAngle = 0.0 * (PI / 180.0)
        val minLength = radius * 0.58f
        drawLine(
            color = tint,
            start = center,
            end = Offset(
                x = center.x + minLength * cos(minAngle).toFloat(),
                y = center.y + minLength * sin(minAngle).toFloat()
            ),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round
        )

        // Second Hand / Thin Accent Hand: pointing to ~1:45 (approx -40 deg)
        val secAngle = (-40.0) * (PI / 180.0)
        val secLength = radius * 0.55f
        drawLine(
            color = tint,
            start = center,
            end = Offset(
                x = center.x + secLength * cos(secAngle).toFloat(),
                y = center.y + secLength * sin(secAngle).toFloat()
            ),
            strokeWidth = w * 0.025f,
            cap = StrokeCap.Round
        )
    }
}
