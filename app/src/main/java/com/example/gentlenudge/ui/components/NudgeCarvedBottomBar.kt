package com.example.gentlenudge.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentlenudge.ui.NudgeView
import kotlin.math.roundToInt

// Softer, lighter, and more modern Nudge blue for selected navigation button
private val ActiveNavBlue = Color(0xFF4579F5)

/**
 * Premium carved bottom navigation bar for Nudge with horizontal sliding active-state ball animation.
 *
 * 1. FIXED AT BOTTOM: Permanently anchored at the bottom of the screen.
 * 2. SMOOTH CARVED NOTCH: The top edge has a smooth concave carved cradle that follows the active pod.
 * 3. 4 ITEMS: Today, Your notes, Hours, Settings.
 * 4. SLIDING BLUE CIRCLE: When the user taps any tab, the softer blue circular active state
 *    smoothly slides horizontally from the previous tab to the newly selected tab with a natural spring easing,
 *    while the navigation bar itself stays completely stationary.
 * 5. UNIFIED ALIGNMENT: All 4 tabs share the exact same icon height, spacing, and label baseline,
 *    so labels never push downward and stay comfortably inside the navigation bar.
 */
@Composable
fun NudgeCarvedBottomBar(
    activeView: NudgeView,
    onViewSelected: (NudgeView) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            NudgeView.TODAY,
            NudgeView.NOTES,
            NudgeView.HOURS,
            NudgeView.SETTINGS
        )
    }

    val activeIndex = items.indexOf(activeView).coerceIn(0, items.lastIndex)

    val barHeight = 64.dp
    val topCrestHeight = 10.dp
    val totalHeight = barHeight + topCrestHeight
    val circleSize = 44.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val horizontalPadding = 12.dp
        val availableWidth = maxWidth - (horizontalPadding * 2)
        val tabWidth = availableWidth / items.size

        val targetOffsetX = (tabWidth * activeIndex) + (tabWidth / 2) - (circleSize / 2)
        val animatedOffsetX by animateDpAsState(
            targetValue = targetOffsetX,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "active_circle_x"
        )

        val isDark = MaterialTheme.colorScheme.background.red < 0.5f
        val navBgColor = if (isDark) {
            Color(0xFA22201D)
        } else {
            Color(0xFAFCFBF9)
        }
        val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.9f)
        val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 2.dp)
                .height(totalHeight)
        ) {
            // Background Canvas with smooth carved cradle contour following animatedOffsetX
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .align(Alignment.BottomCenter)
                    .testTag("carved_bottom_bar_canvas")
            ) {
                val width = size.width
                val height = size.height
                val cornerRadius = 24.dp.toPx()
                val notchRadius = 27.dp.toPx()
                val filletRadius = 13.dp.toPx()

                // Center of the active cradle corresponds to animatedOffsetX + (circleSize / 2)
                val animatedCenterX = (animatedOffsetX + (circleSize / 2)).toPx()

                // Outer pill shape
                val outerPill = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(0f, 0f, width, height),
                            topLeft = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                            topRight = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                            bottomLeft = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                            bottomRight = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                        )
                    )
                }

                // Smooth concave notch path centered at animatedCenterX
                // Localized geometric correction: notch shoulders terminate cleanly before cutting the outer pill's corner arcs
                val notchPath = Path().apply {
                    val safeLeft = cornerRadius
                    val safeRight = width - cornerRadius

                    val availableLeft = (animatedCenterX - safeLeft).coerceAtLeast(0f)
                    val leftScale = (availableLeft / (notchRadius + filletRadius)).coerceIn(0f, 1f)
                    val effectiveLeftNotchRadius = notchRadius * leftScale
                    val effectiveLeftFillet = filletRadius * leftScale

                    val availableRight = (safeRight - animatedCenterX).coerceAtLeast(0f)
                    val rightScale = (availableRight / (notchRadius + filletRadius)).coerceIn(0f, 1f)
                    val effectiveRightNotchRadius = notchRadius * rightScale
                    val effectiveRightFillet = filletRadius * rightScale

                    val cradleLeftX = animatedCenterX - effectiveLeftNotchRadius
                    val cradleRightX = animatedCenterX + effectiveRightNotchRadius
                    val startX = animatedCenterX - (effectiveLeftNotchRadius + effectiveLeftFillet)
                    val endX = animatedCenterX + (effectiveRightNotchRadius + effectiveRightFillet)

                    moveTo(startX, 0f)

                    // Left shoulder curve
                    cubicTo(
                        startX + effectiveLeftFillet * 0.6f, 0f,
                        cradleLeftX, 0f,
                        cradleLeftX, notchRadius * 0.38f
                    )

                    // Deep concave cradle curve
                    cubicTo(
                        cradleLeftX, notchRadius * 0.95f,
                        cradleRightX, notchRadius * 0.95f,
                        cradleRightX, notchRadius * 0.38f
                    )

                    // Right shoulder curve
                    cubicTo(
                        cradleRightX, 0f,
                        endX - effectiveRightFillet * 0.6f, 0f,
                        endX, 0f
                    )

                    lineTo(endX, -25f)
                    lineTo(startX, -25f)
                    close()
                }

                // Carved bar body (pill minus smooth cradle cutout)
                val finalBody = Path().apply {
                    op(outerPill, notchPath, PathOperation.Difference)
                }

                // Shadow
                drawPath(
                    path = finalBody,
                    color = Color(0x10000000),
                    style = Fill
                )

                // Fill translucent background
                drawPath(
                    path = finalBody,
                    color = navBgColor,
                    style = Fill
                )

                // Outer subtle border stroke
                drawPath(
                    path = finalBody,
                    color = borderColor,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Horizontally Sliding Active Softer Blue Circle
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = animatedOffsetX.roundToPx(), y = 2.dp.roundToPx()) }
                    .size(circleSize)
                    .shadow(
                        elevation = 5.dp,
                        shape = CircleShape,
                        spotColor = ActiveNavBlue.copy(alpha = 0.35f),
                        ambientColor = Color(0x14000000)
                    )
                    .clip(CircleShape)
                    .background(ActiveNavBlue)
                    .testTag("sliding_active_indicator"),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = activeView,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "active_icon_crossfade"
                ) { currentActive ->
                    Icon(
                        imageVector = currentActive.icon,
                        contentDescription = currentActive.label,
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            // Interactive Navigation Tabs Row - perfectly aligned vertical layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                items.forEachIndexed { index, view ->
                    val isSelected = activeView == view
                    val label = when (view) {
                        NudgeView.TODAY -> "Today"
                        NudgeView.NOTES -> "Your notes"
                        NudgeView.HOURS -> "Hours"
                        NudgeView.SETTINGS -> "Settings"
                    }

                    val textColor = if (isSelected) ActiveNavBlue else unselectedColor

                    val interactionSource = remember(view) { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onViewSelected(view)
                            }
                            .testTag("nav_tab_${view.name.lowercase()}"),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Fixed icon slot - identical height and center for all tabs
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(circleSize),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isSelected) {
                                    Icon(
                                        imageVector = view.icon,
                                        contentDescription = label,
                                        tint = unselectedColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Uniform label baseline for all tabs
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = textColor,
                                letterSpacing = (-0.1).sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
