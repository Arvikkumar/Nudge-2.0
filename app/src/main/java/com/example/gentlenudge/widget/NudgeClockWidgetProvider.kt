package com.example.gentlenudge.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.widget.RemoteViews
import com.example.gentlenudge.MainActivity
import com.example.gentlenudge.R
import java.util.Calendar

/**
 * NudgeClockWidgetProvider
 *
 * AppWidgetProvider for the Nudge Analog Clock home-screen widget.
 * - Powered by Android's native system-controlled AnalogClock view:
 *   - The second hand, minute hand, and hour hand rotate smoothly and continuously
 *     inside the Launcher process with zero battery overhead.
 *   - Fully immune to background process freezing, low-memory kills, and app closure.
 * - Maintains the signature blue minute-progress ring via a separate, lightweight
 *   minute-level AlarmManager update.
 * - Tapping the widget opens Nudge (MainActivity).
 */
class NudgeClockWidgetProvider : AppWidgetProvider() {

    companion object {
        const val TAG = "NudgeClockWidget"
        const val ACTION_UPDATE_CLOCK_WIDGET = "com.example.gentlenudge.ACTION_UPDATE_CLOCK_WIDGET"
        private const val ALARM_REQUEST_CODE = 9201

        private var isTickerRunning = false
        private var isScreenReceiverRegistered = false

        fun ensureScreenReceiverRegistered(context: Context) {
            isScreenReceiverRegistered = true
        }

        fun unregisterScreenReceiver(context: Context? = null) {
            isScreenReceiverRegistered = false
        }

        fun startTicker(context: Context) {
            // Native AnalogClock runs directly in Launcher process.
            // Ensure next minute alarm is scheduled for the minute progress ring.
            scheduleNextMinuteAlarm(context)
        }

        fun stopTicker() {
            isTickerRunning = false
        }

        fun isTickerActive(): Boolean = isTickerRunning

        fun isScreenReceiverActive(): Boolean = isScreenReceiverRegistered

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, NudgeClockWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val now = Calendar.getInstance()
                for (appWidgetId in appWidgetIds) {
                    updateSingleWidget(context, appWidgetManager, appWidgetId, now)
                }
                scheduleNextMinuteAlarm(context)
            } else {
                cancelMinuteAlarm(context)
            }
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            calendar: Calendar = Calendar.getInstance()
        ) {
            try {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val density = context.resources.displayMetrics.density

                // Render the blue minute-progress ring for this minute on a square high-res canvas
                val ringBitmap = NudgeClockWidgetRenderer.renderProgressRingBitmap(512, 512, calendar)

                val remoteViews = RemoteViews(context.packageName, R.layout.widget_nudge_clock)
                remoteViews.setImageViewBitmap(R.id.widget_clock_image, ringBitmap)

                // Tapping anywhere on the widget opens Nudge
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                remoteViews.setOnClickPendingIntent(R.id.widget_clock_container, pendingIntent)
                remoteViews.setOnClickPendingIntent(R.id.widget_clock_image, pendingIntent)
                remoteViews.setOnClickPendingIntent(R.id.widget_analog_clock, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update widget $appWidgetId: ${e.message}", e)
            }
        }

        fun scheduleNextMinuteAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val updateIntent = Intent(context, NudgeClockWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_CLOCK_WIDGET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Trigger at the top of the next minute (:00 seconds)
            val now = System.currentTimeMillis()
            val nextMinute = ((now / 60000L) + 1) * 60000L

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pendingIntent)
                }
            } catch (se: SecurityException) {
                try {
                    alarmManager.set(AlarmManager.RTC, nextMinute, pendingIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Could not set fallback alarm: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule next minute alarm: ${e.message}", e)
            }
        }

        fun cancelMinuteAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val updateIntent = Intent(context, NudgeClockWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_CLOCK_WIDGET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                updateIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val now = Calendar.getInstance()
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId, now)
        }
        scheduleNextMinuteAlarm(context)
        startTicker(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateSingleWidget(context, appWidgetManager, appWidgetId)
        startTicker(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextMinuteAlarm(context)
        startTicker(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        stopTicker()
        unregisterScreenReceiver(context)
        cancelMinuteAlarm(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == ACTION_UPDATE_CLOCK_WIDGET ||
            action == Intent.ACTION_TIME_TICK ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.TIME_SET"
        ) {
            updateAllWidgets(context)
        }
    }
}
