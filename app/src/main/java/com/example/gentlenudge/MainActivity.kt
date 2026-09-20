package com.example.gentlenudge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gentlenudge.notification.NudgeNotificationHelper
import com.example.gentlenudge.ui.MainScreen
import com.example.gentlenudge.ui.theme.GentleNudgeTheme
import com.example.gentlenudge.ui.theme.ThemeMode
import com.example.gentlenudge.ui.viewmodel.NudgeViewModel
import com.example.gentlenudge.ui.viewmodel.NudgeViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: NudgeViewModel by viewModels {
        val app = application as GentleNudgeApp
        NudgeViewModelFactory(app, app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        try {
            // Create notification channels for nudges and backups
            NudgeNotificationHelper.createNotificationChannels(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to create notification channels: ${e.message}")
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DisposableEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { useDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { useDarkTheme }
                )
                onDispose {}
            }

            GentleNudgeTheme(darkTheme = useDarkTheme) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            com.example.gentlenudge.notification.NudgeEventNotificationScheduler.rescheduleIfEnabled(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to re-evaluate event reminders: ${e.message}")
        }
    }
}
