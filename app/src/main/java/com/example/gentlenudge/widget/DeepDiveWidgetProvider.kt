package com.example.gentlenudge.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.example.gentlenudge.MainActivity
import com.example.gentlenudge.R
import com.example.gentlenudge.deepdive.DeepDiveManager

/**
 * DeepDiveWidgetProvider
 *
 * AppWidgetProvider for Nudge's "Deep Dive" home-screen widget.
 * - Displays a compact, balanced idle state with quick-start buttons (30 min, 1 hour, play).
 * - Switches to an active state when a session is in progress, displaying
 *   the live countdown via Android's native Chronometer and target end time.
 * - Provides a Stop button to end the session immediately.
 * - Displays reminder count and notification style labels directly from DeepDiveManager.
 * - Directly opens the appropriate section of Deep Dive configuration upon interaction.
 * - Relies strictly on DeepDiveManager as the single source of truth.
 */
class DeepDiveWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "DeepDiveWidget"

        const val ACTION_START_DEEP_DIVE_30 = "com.example.gentlenudge.ACTION_START_DEEP_DIVE_30"
        const val ACTION_START_DEEP_DIVE_60 = "com.example.gentlenudge.ACTION_START_DEEP_DIVE_60"
        const val ACTION_STOP_DEEP_DIVE = "com.example.gentlenudge.ACTION_STOP_DEEP_DIVE"
        const val ACTION_UPDATE_DEEP_DIVE_WIDGET = "com.example.gentlenudge.ACTION_UPDATE_DEEP_DIVE_WIDGET"
        const val ACTION_OPEN_DEEP_DIVE_CONFIG = "com.example.gentlenudge.ACTION_OPEN_DEEP_DIVE_CONFIG"

        private const val RC_START_30 = 9301
        private const val RC_START_60 = 9302
        private const val RC_START_QUICK = 9303
        private const val RC_STOP = 9304
        private const val RC_LAUNCH_APP = 9305
        private const val RC_OPEN_CONFIG = 9306
        private const val RC_OPEN_SOUND_CONFIG = 9307

        /**
         * Triggers an immediate refresh for all active instances of the Deep Dive widget.
         */
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, DeepDiveWidgetProvider::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (widgetIds != null && widgetIds.isNotEmpty()) {
                    for (appWidgetId in widgetIds) {
                        updateWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to updateAllWidgets: ${e.message}", e)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                DeepDiveManager.init(context)
                val state = DeepDiveManager.state.value
                val now = System.currentTimeMillis()
                val isActuallyActive = state.isActive && state.endTimeMillis > now

                // Intent to open MainActivity when tapping idle card background
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val launchPendingIntent = PendingIntent.getActivity(
                    context,
                    RC_LAUNCH_APP,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Shared info: Reminder count and Notification style from DeepDiveManager
                val remindersCount = if (isActuallyActive) {
                    state.reminderPoints.size
                } else {
                    DeepDiveManager.getConfiguredRemindersCount(context)
                }
                val remindersLabel = DeepDiveManager.formatRemindersCountLabel(remindersCount)

                val notificationStyle = if (isActuallyActive) {
                    state.notificationStyle
                } else {
                    DeepDiveManager.getSavedNotificationStyle(context)
                }
                val styleLabel = DeepDiveManager.formatNotificationStyleLabel(notificationStyle)

                // General config intent (opens config sheet)
                val configIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_OPEN_DEEP_DIVE_CONFIG
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("EXTRA_OPEN_DEEP_DIVE_CONFIG", true)
                }
                val configPendingIntent = PendingIntent.getActivity(
                    context,
                    RC_OPEN_CONFIG,
                    configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Sound / Ringtone config intent (opens config sheet specifically at Notification Style)
                val soundConfigIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_OPEN_DEEP_DIVE_CONFIG
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("EXTRA_OPEN_DEEP_DIVE_CONFIG", true)
                    putExtra("EXTRA_CONFIG_SECTION", "sound")
                }
                val soundConfigPendingIntent = PendingIntent.getActivity(
                    context,
                    RC_OPEN_SOUND_CONFIG,
                    soundConfigIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 30 min quick-start action
                val start30Intent = Intent(context, DeepDiveWidgetProvider::class.java).apply {
                    action = ACTION_START_DEEP_DIVE_30
                }
                val start30PendingIntent = PendingIntent.getBroadcast(
                    context,
                    RC_START_30,
                    start30Intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 1 hour quick-start action
                val start60Intent = Intent(context, DeepDiveWidgetProvider::class.java).apply {
                    action = ACTION_START_DEEP_DIVE_60
                }
                val start60PendingIntent = PendingIntent.getBroadcast(
                    context,
                    RC_START_60,
                    start60Intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Play button (quick start 30 min)
                val quickPlayIntent = Intent(context, DeepDiveWidgetProvider::class.java).apply {
                    action = ACTION_START_DEEP_DIVE_30
                }
                val quickPlayPendingIntent = PendingIntent.getBroadcast(
                    context,
                    RC_START_QUICK,
                    quickPlayIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Stop button action
                val stopIntent = Intent(context, DeepDiveWidgetProvider::class.java).apply {
                    action = ACTION_STOP_DEEP_DIVE
                }
                val stopPendingIntent = PendingIntent.getBroadcast(
                    context,
                    RC_STOP,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                fun bindWidgetViews(remoteViews: RemoteViews) {
                    if (isActuallyActive) {
                        // -------------------------------------------------------------
                        // ACTIVE STATE
                        // -------------------------------------------------------------
                        remoteViews.setViewVisibility(R.id.widget_deep_dive_idle_container, View.GONE)
                        remoteViews.setViewVisibility(R.id.widget_deep_dive_active_container, View.VISIBLE)
                        remoteViews.setInt(
                            R.id.widget_deep_dive_root,
                            "setBackgroundResource",
                            R.drawable.bg_widget_deep_dive_active
                        )

                        // Target end time text (e.g. "Ends at 4:30 PM")
                        val clockTimeStr = DeepDiveManager.formatClockTime(state.endTimeMillis)
                        remoteViews.setTextViewText(R.id.widget_active_end_time, "Ends at $clockTimeStr")

                        // Native Chronometer countdown
                        val millisRemaining = state.endTimeMillis - now
                        val baseUptime = SystemClock.elapsedRealtime() + millisRemaining
                        remoteViews.setChronometer(R.id.widget_active_chronometer, baseUptime, null, true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            remoteViews.setChronometerCountDown(R.id.widget_active_chronometer, true)
                        }

                        // Bottom info surface (Active)
                        remoteViews.setTextViewText(R.id.widget_active_reminders_text, remindersLabel)
                        remoteViews.setTextViewText(R.id.widget_active_sound_text, styleLabel)
                        remoteViews.setOnClickPendingIntent(R.id.widget_active_reminders_text, configPendingIntent)
                        remoteViews.setOnClickPendingIntent(R.id.widget_active_sound_text, soundConfigPendingIntent)
                        remoteViews.setOnClickPendingIntent(R.id.widget_active_btn_config, configPendingIntent)

                        // Stop button action
                        remoteViews.setOnClickPendingIntent(R.id.widget_btn_stop, stopPendingIntent)

                        // Tapping root in active mode opens app to view active session
                        remoteViews.setOnClickPendingIntent(R.id.widget_deep_dive_root, launchPendingIntent)
                    } else {
                        // -------------------------------------------------------------
                        // IDLE STATE
                        // -------------------------------------------------------------
                        remoteViews.setViewVisibility(R.id.widget_deep_dive_idle_container, View.VISIBLE)
                        remoteViews.setViewVisibility(R.id.widget_deep_dive_active_container, View.GONE)
                        remoteViews.setInt(
                            R.id.widget_deep_dive_root,
                            "setBackgroundResource",
                            R.drawable.bg_widget_deep_dive_card
                        )

                        // Quick-start controls
                        remoteViews.setOnClickPendingIntent(R.id.widget_btn_30min, start30PendingIntent)
                        remoteViews.setOnClickPendingIntent(R.id.widget_btn_1hour, start60PendingIntent)
                        remoteViews.setOnClickPendingIntent(R.id.widget_btn_quick_play, quickPlayPendingIntent)

                        // Bottom info surface (Idle)
                        remoteViews.setTextViewText(R.id.widget_idle_reminders_text, remindersLabel)
                        remoteViews.setTextViewText(R.id.widget_idle_sound_text, styleLabel)
                        remoteViews.setOnClickPendingIntent(R.id.widget_idle_reminders_text, configPendingIntent)
                        remoteViews.setOnClickPendingIntent(R.id.widget_idle_sound_text, soundConfigPendingIntent)
                        remoteViews.setOnClickPendingIntent(R.id.widget_idle_btn_config, configPendingIntent)

                        // Tapping root opens the app
                        remoteViews.setOnClickPendingIntent(R.id.widget_deep_dive_root, launchPendingIntent)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val normalViews = RemoteViews(context.packageName, R.layout.widget_deep_dive)
                    val shortViews = RemoteViews(context.packageName, R.layout.widget_deep_dive_short)
                    bindWidgetViews(normalViews)
                    bindWidgetViews(shortViews)
                    val responsiveViews = RemoteViews(
                        mapOf(
                            SizeF(160f, 60f) to shortViews,
                            SizeF(160f, 115f) to normalViews
                        )
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, responsiveViews)
                } else {
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
                    val layoutId = if (minHeight in 1..114) R.layout.widget_deep_dive_short else R.layout.widget_deep_dive
                    val views = RemoteViews(context.packageName, layoutId)
                    bindWidgetViews(views)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update widget $appWidgetId: ${e.message}", e)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        when (action) {
            ACTION_START_DEEP_DIVE_30 -> {
                startDeepDiveWithDuration(context, 30L * 60L * 1000L)
            }
            ACTION_START_DEEP_DIVE_60 -> {
                startDeepDiveWithDuration(context, 60L * 60L * 1000L)
            }
            ACTION_STOP_DEEP_DIVE -> {
                DeepDiveManager.endSession(context)
                updateAllWidgets(context)
            }
            ACTION_UPDATE_DEEP_DIVE_WIDGET,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                updateAllWidgets(context)
            }
        }
    }

    private fun startDeepDiveWithDuration(context: Context, durationMillis: Long) {
        DeepDiveManager.init(context)
        val state = DeepDiveManager.state.value
        val now = System.currentTimeMillis()

        // Concurrency safeguard: If already running, do not re-trigger or create duplicate alarms
        if (state.isActive && state.endTimeMillis > now) {
            updateAllWidgets(context)
            return
        }

        val targetEndTime = now + durationMillis
        val savedStyle = DeepDiveManager.getSavedNotificationStyle(context)
        DeepDiveManager.startSession(
            context = context,
            targetEndTimeMillis = targetEndTime,
            notificationStyle = savedStyle
        )
        updateAllWidgets(context)
    }
}
