package com.example.gentlenudge.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Native Jetpack Compose Day / Night theme toggle switch.
 * Visual reference inspired by Uiverse: RiccardoRapelli/jolly-chicken-91.
 *
 * Light State:
 * - Nudge sky blue track
 * - Warm golden sun thumb with subtle rays & glow
 * - Soft fluffy white clouds on the lower right
 *
 * Dark State:
 * - Charcoal night sky track
 * - Lunar off-white moon thumb with subtle craters
 * - Sparkle stars and twinkling star dots in the night sky
 *
 * Smooth animated transition between states.
 */
@Composable
fun ThemeToggleSwitch(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 48.dp,
    height: Dp = 26.dp,
    thumbSize: Dp = 20.dp
) {
    val padding = (height - thumbSize) / 2
    val maxThumbOffset = width - thumbSize - padding

    // Animation specifications: 90ms duration for immediate, crisp tactile response
    val animDuration = 90

    val thumbOffsetAnim by animateDpAsState(
        targetValue = if (isDark) maxThumbOffset else padding,
        animationSpec = tween(durationMillis = animDuration, easing = FastOutSlowInEasing),
        label = "theme_thumb_offset"
    )

    // Snap colors and element alphas immediately with the theme change to prevent muddy intermediate frames
    val trackBgColor = if (isDark) Color(0xFF151922) else Color(0xFF508CE0)
    val trackBorderColor = if (isDark) Color(0xFF2C3545) else Color(0xFF7EACEC)
    val thumbColor = if (isDark) Color(0xFFF0F3F6) else Color(0xFFFFCA28)

    val thumbRotation by animateFloatAsState(
        targetValue = if (isDark) 360f else 0f,
        animationSpec = tween(durationMillis = animDuration, easing = FastOutSlowInEasing),
        label = "theme_thumb_rotation"
    )

    val darkAlpha = if (isDark) 1f else 0f
    val lightAlpha = if (isDark) 0f else 1f

    val interactionSource = remember { MutableInteractionSource() }
    val accessibleDescription = if (isDark) "Switch to light theme" else "Switch to dark theme"

    // Outer touch container (at least 48dp height for accessible touch target)
    Box(
        modifier = modifier
            .size(width = width + 8.dp, height = 48.dp)
            .semantics {
                role = Role.Switch
                contentDescription = accessibleDescription
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 24.dp),
                onClick = onToggle
            )
            .testTag("theme_toggle_switch"),
        contentAlignment = Alignment.Center
    ) {
        // Track Pill
        Box(
            modifier = Modifier
                .size(width = width, height = height)
                .clip(CircleShape)
                .background(trackBgColor)
                .border(0.8.dp, trackBorderColor, CircleShape)
        ) {
            // Background Canvas: Clouds (Light state) & Stars (Dark state)
            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                val canvasW = size.width
                val canvasH = size.height

                // --- 1. LIGHT MODE: Fluffy Cloud Elements (bottom-right) ---
                if (lightAlpha > 0.01f) {
                    val sinkOffsetY = (1f - lightAlpha) * 10.dp.toPx()
                    val cloudAlpha = lightAlpha.coerceIn(0f, 1f)

                    // Secondary soft blue/white backing puff
                    drawCircle(
                        color = Color(0xFFD4E6FA).copy(alpha = 0.65f * cloudAlpha),
                        radius = 4.2.dp.toPx(),
                        center = Offset(canvasW * 0.74f, (canvasH * 0.62f) + sinkOffsetY)
                    )

                    // Primary white cloud puffs
                    drawCircle(
                        color = Color.White.copy(alpha = 0.95f * cloudAlpha),
                        radius = 4.0.dp.toPx(),
                        center = Offset(canvasW * 0.67f, (canvasH * 0.78f) + sinkOffsetY)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.95f * cloudAlpha),
                        radius = 5.2.dp.toPx(),
                        center = Offset(canvasW * 0.81f, (canvasH * 0.70f) + sinkOffsetY)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.95f * cloudAlpha),
                        radius = 3.6.dp.toPx(),
                        center = Offset(canvasW * 0.93f, (canvasH * 0.80f) + sinkOffsetY)
                    )

                    // Rounded base pill to merge puffs at bottom edge
                    val cloudBasePath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = canvasW * 0.62f,
                                top = (canvasH * 0.72f) + sinkOffsetY,
                                right = canvasW * 0.98f,
                                bottom = (canvasH * 0.98f) + sinkOffsetY,
                                cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())
                            )
                        )
                    }
                    drawPath(
                        path = cloudBasePath,
                        color = Color.White.copy(alpha = 0.95f * cloudAlpha),
                        style = Fill
                    )
                }

                // --- 2. DARK MODE: Stars & Sparkles (left / center) ---
                if (darkAlpha > 0.01f) {
                    val starsAlpha = darkAlpha.coerceIn(0f, 1f)
                    val starColor = Color.White.copy(alpha = 0.95f * starsAlpha)
                    val warmStarColor = Color(0xFFFFF9C4).copy(alpha = 0.90f * starsAlpha)
                    val subtleStarColor = Color(0xFFCFD8DC).copy(alpha = 0.75f * starsAlpha)

                    // Star 1: 4-pointed sparkle star at (10.5.dp, 8.5.dp)
                    val sparkleCenter = Offset(10.5.dp.toPx(), 8.2.dp.toPx())
                    val sparkleArm = 2.4.dp.toPx() * darkAlpha
                    drawLine(
                        color = starColor,
                        start = Offset(sparkleCenter.x - sparkleArm, sparkleCenter.y),
                        end = Offset(sparkleCenter.x + sparkleArm, sparkleCenter.y),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = starColor,
                        start = Offset(sparkleCenter.x, sparkleCenter.y - sparkleArm),
                        end = Offset(sparkleCenter.x, sparkleCenter.y + sparkleArm),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = starColor,
                        radius = 0.75.dp.toPx() * darkAlpha,
                        center = sparkleCenter
                    )

                    // Star 2: Warm yellow star dot at (18.dp, 7.dp)
                    drawCircle(
                        color = warmStarColor,
                        radius = 1.1.dp.toPx() * darkAlpha,
                        center = Offset(18.dp.toPx(), 6.8.dp.toPx())
                    )

                    // Star 3: Soft star dot at (13.dp, 17.5.dp)
                    drawCircle(
                        color = starColor,
                        radius = 0.95.dp.toPx() * darkAlpha,
                        center = Offset(13.dp.toPx(), 17.5.dp.toPx())
                    )

                    // Star 4: Subtle star dot at (21.5.dp, 17.5.dp)
                    drawCircle(
                        color = subtleStarColor,
                        radius = 0.85.dp.toPx() * darkAlpha,
                        center = Offset(21.5.dp.toPx(), 17.5.dp.toPx())
                    )

                    // Star 5: Tiny distant twinkle at (6.dp, 16.5.dp)
                    drawCircle(
                        color = starColor,
                        radius = 0.7.dp.toPx() * darkAlpha,
                        center = Offset(6.2.dp.toPx(), 16.5.dp.toPx())
                    )
                }
            }

            // The Sliding Thumb (Sun ↔ Moon)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = thumbOffsetAnim.roundToPx(),
                            y = padding.roundToPx()
                        )
                    }
                    .size(thumbSize)
                    .graphicsLayer {
                        rotationZ = thumbRotation
                    }
                    .shadow(
                        elevation = if (isDark) 1.5.dp else 2.5.dp,
                        shape = CircleShape,
                        spotColor = if (isDark) Color(0xFF000000) else Color(0x66FF8F00)
                    )
                    .clip(CircleShape)
                    .background(thumbColor)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val thumbW = size.width
                    val thumbH = size.height
                    val thumbCenter = Offset(thumbW / 2, thumbH / 2)

                    // --- SUN DETAILS (Light Mode) ---
                    if (lightAlpha > 0.01f) {
                        val sunAlpha = lightAlpha.coerceIn(0f, 1f)

                        // Subtle warm sun inner glow
                        drawCircle(
                            color = Color(0xFFFFA000).copy(alpha = 0.22f * sunAlpha),
                            radius = (thumbW * 0.46f),
                            center = Offset(thumbCenter.x + 0.8.dp.toPx(), thumbCenter.y + 0.8.dp.toPx())
                        )

                        // Subtle outer ray corona
                        drawCircle(
                            color = Color(0xFFFFE082).copy(alpha = 0.35f * sunAlpha),
                            radius = (thumbW * 0.48f),
                            center = thumbCenter,
                            style = Stroke(width = 0.9.dp.toPx())
                        )
                    }

                    // --- MOON DETAILS: CRATERS (Dark Mode) ---
                    if (darkAlpha > 0.01f) {
                        val moonAlpha = darkAlpha.coerceIn(0f, 1f)
                        val craterColorLarge = Color(0xFFCBD5E1).copy(alpha = 0.95f * moonAlpha)
                        val craterColorSmall = Color(0xFFD4DEE7).copy(alpha = 0.90f * moonAlpha)

                        // Crater 1 (Upper left)
                        drawCircle(
                            color = craterColorLarge,
                            radius = 2.0.dp.toPx() * darkAlpha,
                            center = Offset(thumbW * 0.32f, thumbH * 0.32f)
                        )

                        // Crater 2 (Right side)
                        drawCircle(
                            color = craterColorSmall,
                            radius = 1.6.dp.toPx() * darkAlpha,
                            center = Offset(thumbW * 0.65f, thumbH * 0.52f)
                        )

                        // Crater 3 (Lower left)
                        drawCircle(
                            color = craterColorLarge,
                            radius = 1.25.dp.toPx() * darkAlpha,
                            center = Offset(thumbW * 0.38f, thumbH * 0.70f)
                        )
                    }
                }
            }
        }
    }
}
