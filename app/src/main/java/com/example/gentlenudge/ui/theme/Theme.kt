package com.example.gentlenudge.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LightColorScheme = lightColorScheme(
    primary = NudgeBlue,
    onPrimary = Color.White,
    primaryContainer = NudgeBlueContainer,
    onPrimaryContainer = OnNudgeBlueContainer,
    secondary = Color(0xFF5E6E82),
    onSecondary = Color.White,
    secondaryContainer = SoftSage,
    onSecondaryContainer = Color(0xFF1E3A24),
    tertiary = Color(0xFF8B6B58),
    background = PaperBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = PaperCardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF2EFE8),
    onSurfaceVariant = TextSecondaryLight,
    outline = PaperBorderLight,
    outlineVariant = Color(0xFFDFD9CE)
)

val DarkColorScheme = darkColorScheme(
    primary = NudgeBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1C2D5A),
    onPrimaryContainer = Color(0xFFB0C6FF),
    secondary = Color(0xFF9EACB8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF263328),
    onSecondaryContainer = Color(0xFFCCE8D0),
    tertiary = Color(0xFFD4B8A5),
    background = PaperBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = PaperCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF282520),
    onSurfaceVariant = TextSecondaryDark,
    outline = PaperBorderDark,
    outlineVariant = Color(0xFF3D3932)
)

@Composable
fun GentleNudgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // Light theme: dark system icons; Dark theme: light system icons
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
