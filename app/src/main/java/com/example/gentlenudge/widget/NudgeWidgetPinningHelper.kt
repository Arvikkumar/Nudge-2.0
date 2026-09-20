package com.example.gentlenudge.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.example.gentlenudge.R

/**
 * NudgeWidgetPinningHelper
 *
 * Safely wraps Android's AppWidgetManager.requestPinAppWidget API.
 * - Checks whether the active launcher supports direct widget pinning.
 * - If supported, invokes requestPinAppWidget() to display the system confirmation UI.
 * - If unsupported, displays a fallback guidance message instructing the user
 *   to add the Nudge Clock via the home screen Widgets menu.
 */
object NudgeWidgetPinningHelper {

    private const val TAG = "NudgeWidgetPinning"

    fun isPinningSupported(context: Context): Boolean {
        return try {
            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                ?: AppWidgetManager.getInstance(context)
                ?: return false
            appWidgetManager.isRequestPinAppWidgetSupported
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if widget pinning is supported: ${e.message}", e)
            false
        }
    }

    fun requestPinClockWidget(
        context: Context,
        onFallbackMessage: ((String) -> Unit)? = null
    ): Boolean {
        val fallbackText = try {
            context.getString(R.string.widget_pin_fallback_message)
        } catch (e: Exception) {
            "Your home screen launcher does not support direct pinning. You can add the Nudge Clock via your home screen's Widgets menu."
        }
        try {
            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                ?: AppWidgetManager.getInstance(context)

            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                val provider = ComponentName(context, NudgeClockWidgetProvider::class.java)
                val success = appWidgetManager.requestPinAppWidget(provider, null, null)
                if (success) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while requesting widget pinning: ${e.message}", e)
        }

        // Fallback when unsupported or request failed
        if (onFallbackMessage != null) {
            onFallbackMessage(fallbackText)
        } else {
            Toast.makeText(context, fallbackText, Toast.LENGTH_LONG).show()
        }
        return false
    }
}
