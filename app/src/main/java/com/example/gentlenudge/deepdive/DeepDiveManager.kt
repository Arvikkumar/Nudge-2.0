package com.example.gentlenudge.deepdive

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gentlenudge.MainActivity
import com.example.gentlenudge.notification.NudgeNotificationHelper
import com.example.gentlenudge.notification.NudgeNotificationReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Manages the state, persistence, alarms, and notifications for the reusable Deep Dive feature.
 */
object DeepDiveManager {

    private const val TAG = "DeepDiveManager"
    private const val PREFS_NAME = "deep_dive_prefs"
    private const val KEY_END_TIME_MILLIS = "deep_dive_end_time_millis"
    private const val KEY_STYLE = "deep_dive_notification_style"
    private const val KEY_REMINDER_POINTS = "deep_dive_reminder_points"
    private const val DEFAULT_STYLE = "One Shot" // "One Shot" or "Full Ringtone"

    const val DEEP_DIVE_ALARM_REQUEST_CODE = 998877
    const val DEEP_DIVE_REMINDER_BASE_REQUEST_CODE = 998900
    const val DEEP_DIVE_NOTIFICATION_ID = 998877
    const val DEEP_DIVE_REMINDER_NOTIFICATION_ID = 998879

    const val ACTION_FIRE_DEEP_DIVE = "com.example.gentlenudge.ACTION_FIRE_DEEP_DIVE"
    const val ACTION_DISMISS_DEEP_DIVE = "com.example.gentlenudge.ACTION_DISMISS_DEEP_DIVE"

    const val EXTRA_IS_REMINDER = "deep_dive_is_reminder"
    const val EXTRA_REMINDER_TIME_MILLIS = "deep_dive_reminder_time_millis"
    const val EXTRA_REMINDER_LABEL = "deep_dive_reminder_label"

    data class ReminderPoint(
        val triggerTimeMillis: Long,
        val label: String
    )

    data class DeepDiveState(
        val isActive: Boolean = false,
        val endTimeMillis: Long = 0L,
        val notificationStyle: String = DEFAULT_STYLE,
        val reminderPoints: List<ReminderPoint> = emptyList()
    )

    private val _state = MutableStateFlow(DeepDiveState())
    val state: StateFlow<DeepDiveState> = _state.asStateFlow()

    private var _pendingReminders: List<ReminderPoint> = emptyList()

    fun setPendingReminders(reminders: List<ReminderPoint>) {
        _pendingReminders = reminders
    }

    fun consumePendingReminders(): List<ReminderPoint> {
        val result = _pendingReminders
        _pendingReminders = emptyList()
        return result
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun serializeReminders(reminders: List<ReminderPoint>): String {
        val array = JSONArray()
        for (r in reminders) {
            val obj = JSONObject().apply {
                put("time", r.triggerTimeMillis)
                put("label", r.label)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeReminders(json: String?): List<ReminderPoint> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<ReminderPoint>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val time = obj.getLong("time")
                val label = obj.optString("label", "Reminder")
                list.add(ReminderPoint(time, label))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse reminders: ${e.message}")
        }
        return list.sortedBy { it.triggerTimeMillis }
    }

    /**
     * Initialize/sync state from SharedPreferences (called on app startup or UI mount).
     */
    fun init(context: Context) {
        val prefs = getPrefs(context)
        val endTime = prefs.getLong(KEY_END_TIME_MILLIS, 0L)
        val style = prefs.getString(KEY_STYLE, DEFAULT_STYLE) ?: DEFAULT_STYLE
        val remindersJson = prefs.getString(KEY_REMINDER_POINTS, null)
        val allReminders = deserializeReminders(remindersJson)
        val now = System.currentTimeMillis()

        if (endTime > now) {
            val futureReminders = allReminders.filter { it.triggerTimeMillis > now }
            _state.value = DeepDiveState(
                isActive = true,
                endTimeMillis = endTime,
                notificationStyle = style,
                reminderPoints = futureReminders
            )
        } else {
            if (endTime != 0L) {
                // Was active in past, reset
                prefs.edit()
                    .remove(KEY_END_TIME_MILLIS)
                    .remove(KEY_REMINDER_POINTS)
                    .apply()
            }
            _state.value = DeepDiveState(
                isActive = false,
                endTimeMillis = 0L,
                notificationStyle = style,
                reminderPoints = emptyList()
            )
        }
    }

    fun getSavedNotificationStyle(context: Context): String {
        return getPrefs(context).getString(KEY_STYLE, DEFAULT_STYLE) ?: DEFAULT_STYLE
    }

    /**
     * Start a new Deep Dive session ending at targetEndTimeMillis.
     * Optionally accepts intermediate reminder points.
     */
    fun startSession(
        context: Context,
        targetEndTimeMillis: Long,
        notificationStyle: String,
        reminders: List<ReminderPoint> = consumePendingReminders()
    ) {
        val now = System.currentTimeMillis()
        val validReminders = reminders
            .filter { it.triggerTimeMillis > now && it.triggerTimeMillis < targetEndTimeMillis }
            .distinctBy { it.triggerTimeMillis }
            .sortedBy { it.triggerTimeMillis }

        val prefs = getPrefs(context)
        prefs.edit()
            .putLong(KEY_END_TIME_MILLIS, targetEndTimeMillis)
            .putString(KEY_STYLE, notificationStyle)
            .putString(KEY_REMINDER_POINTS, serializeReminders(validReminders))
            .apply()

        _state.value = DeepDiveState(
            isActive = true,
            endTimeMillis = targetEndTimeMillis,
            notificationStyle = notificationStyle,
            reminderPoints = validReminders
        )

        // Clear any previous alarms first
        cancelAllAlarms(context)

        // Schedule the final completion alarm
        scheduleAlarm(context, targetEndTimeMillis, notificationStyle)

        // Schedule each intermediate reminder alarm
        for ((index, reminder) in validReminders.withIndex()) {
            scheduleReminderAlarm(context, reminder.triggerTimeMillis, reminder.label, notificationStyle, index)
        }
    }

    /**
     * End session manually (or cancel).
     */
    fun endSession(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit()
            .remove(KEY_END_TIME_MILLIS)
            .remove(KEY_REMINDER_POINTS)
            .apply()

        cancelAllAlarms(context)
        dismissNotification(context, DEEP_DIVE_NOTIFICATION_ID)
        dismissNotification(context, DEEP_DIVE_REMINDER_NOTIFICATION_ID)

        _state.value = _state.value.copy(
            isActive = false,
            endTimeMillis = 0L,
            reminderPoints = emptyList()
        )
    }

    private fun scheduleAlarm(context: Context, triggerAtMillis: Long, style: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = ACTION_FIRE_DEEP_DIVE
            putExtra(KEY_STYLE, style)
            putExtra(EXTRA_IS_REMINDER, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DEEP_DIVE_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled Deep Dive final alarm for $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied for exact alarm, falling back: ${e.message}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule Deep Dive alarm: ${e.message}", e)
        }
    }

    private fun scheduleReminderAlarm(
        context: Context,
        triggerAtMillis: Long,
        label: String,
        style: String,
        index: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = ACTION_FIRE_DEEP_DIVE
            putExtra(KEY_STYLE, style)
            putExtra(EXTRA_IS_REMINDER, true)
            putExtra(EXTRA_REMINDER_TIME_MILLIS, triggerAtMillis)
            putExtra(EXTRA_REMINDER_LABEL, label)
        }

        val requestCode = DEEP_DIVE_REMINDER_BASE_REQUEST_CODE + (index % 50)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled Deep Dive reminder alarm for $triggerAtMillis (index $index, label $label)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied for exact alarm, falling back: ${e.message}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule Deep Dive reminder alarm: ${e.message}", e)
        }
    }

    private fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancel final completion alarm
        val finalIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = ACTION_FIRE_DEEP_DIVE
        }
        val finalPendingIntent = PendingIntent.getBroadcast(
            context,
            DEEP_DIVE_ALARM_REQUEST_CODE,
            finalIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (finalPendingIntent != null) {
            alarmManager.cancel(finalPendingIntent)
            finalPendingIntent.cancel()
        }

        // Cancel reminder alarms
        for (i in 0 until 50) {
            val reminderIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
                action = ACTION_FIRE_DEEP_DIVE
            }
            val reminderPendingIntent = PendingIntent.getBroadcast(
                context,
                DEEP_DIVE_REMINDER_BASE_REQUEST_CODE + i,
                reminderIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (reminderPendingIntent != null) {
                alarmManager.cancel(reminderPendingIntent)
                reminderPendingIntent.cancel()
            }
        }
    }

    /**
     * Reschedule alarm on phone reboot if session is still in the future.
     */
    fun rescheduleOnBoot(context: Context) {
        val prefs = getPrefs(context)
        val endTime = prefs.getLong(KEY_END_TIME_MILLIS, 0L)
        val style = prefs.getString(KEY_STYLE, DEFAULT_STYLE) ?: DEFAULT_STYLE
        val now = System.currentTimeMillis()

        if (endTime > now) {
            scheduleAlarm(context, endTime, style)
            Log.d(TAG, "Rescheduled active Deep Dive alarm on boot for $endTime")

            val reminders = deserializeReminders(prefs.getString(KEY_REMINDER_POINTS, null))
            val futureReminders = reminders.filter { it.triggerTimeMillis > now }
            for ((index, reminder) in futureReminders.withIndex()) {
                scheduleReminderAlarm(context, reminder.triggerTimeMillis, reminder.label, style, index)
                Log.d(TAG, "Rescheduled active Deep Dive reminder alarm on boot for ${reminder.triggerTimeMillis}")
            }
        } else if (endTime != 0L) {
            prefs.edit()
                .remove(KEY_END_TIME_MILLIS)
                .remove(KEY_REMINDER_POINTS)
                .apply()
        }
    }

    /**
     * Triggered when an intermediate reminder alarm fires.
     * Keeps the session active, updates remaining reminders, and posts a reminder notification.
     */
    fun onReminderAlarmFired(context: Context, reminderTimeMillis: Long, label: String, style: String) {
        val prefs = getPrefs(context)
        val endTime = prefs.getLong(KEY_END_TIME_MILLIS, 0L)
        val now = System.currentTimeMillis()

        if (endTime <= now) {
            return
        }

        // Prune the fired reminder from persistent storage
        val currentReminders = deserializeReminders(prefs.getString(KEY_REMINDER_POINTS, null))
        val updatedReminders = currentReminders.filter { it.triggerTimeMillis != reminderTimeMillis && it.triggerTimeMillis > now }
        prefs.edit().putString(KEY_REMINDER_POINTS, serializeReminders(updatedReminders)).apply()

        // Keep session active with remaining reminders
        _state.value = _state.value.copy(
            isActive = true,
            endTimeMillis = endTime,
            reminderPoints = updatedReminders
        )

        showReminderNotification(context, label, style, endTime)
    }

    /**
     * Triggered when the final Deep Dive completion alarm fires.
     */
    fun onAlarmFired(context: Context, style: String) {
        // Reset active state in prefs & state flow so card returns to reusable
        val prefs = getPrefs(context)
        prefs.edit()
            .remove(KEY_END_TIME_MILLIS)
            .remove(KEY_REMINDER_POINTS)
            .apply()
        _state.value = _state.value.copy(
            isActive = false,
            endTimeMillis = 0L,
            reminderPoints = emptyList()
        )

        // Show the notification according to selected style
        showNotification(context, style)
    }

    private fun showReminderNotification(
        context: Context,
        label: String,
        style: String,
        endTimeMillis: Long
    ) {
        NudgeNotificationHelper.createNotificationChannels(context)

        val isFullRingtone = style.equals("Full Ringtone", ignoreCase = true)
        val channelId = if (isFullRingtone) {
            NudgeNotificationHelper.FULL_RINGTONE_CHANNEL_ID
        } else {
            NudgeNotificationHelper.GENTLE_CHANNEL_ID
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            DEEP_DIVE_REMINDER_NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = ACTION_DISMISS_DEEP_DIVE
            putExtra("notification_id", DEEP_DIVE_REMINDER_NOTIFICATION_ID)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            DEEP_DIVE_REMINDER_NOTIFICATION_ID + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingStr = formatTimeRemaining(endTimeMillis)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Deep Dive Reminder")
            .setContentText("$label · $remainingStr")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Deep Dive reminder: $label reached. $remainingStr."
                )
            )
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        if (isFullRingtone) {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(longArrayOf(0, 700, 300, 700))
        } else {
            val notificationSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setSound(notificationSound)
                .setVibrate(longArrayOf(0, 250))
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(DEEP_DIVE_REMINDER_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post Deep Dive reminder notification: ${e.message}", e)
        }
    }

    private fun showNotification(context: Context, style: String) {
        NudgeNotificationHelper.createNotificationChannels(context)

        val isFullRingtone = style.equals("Full Ringtone", ignoreCase = true)
        val channelId = if (isFullRingtone) {
            NudgeNotificationHelper.FULL_RINGTONE_CHANNEL_ID
        } else {
            NudgeNotificationHelper.GENTLE_CHANNEL_ID
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            DEEP_DIVE_NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = ACTION_DISMISS_DEEP_DIVE
            putExtra("notification_id", DEEP_DIVE_NOTIFICATION_ID)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            DEEP_DIVE_NOTIFICATION_ID + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Deep Dive Complete")
            .setContentText("Your session has finished.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Deep Dive complete · Take a gentle breath and return to your rhythm."
                )
            )
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        if (isFullRingtone) {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(longArrayOf(0, 700, 300, 700, 300, 700))
        } else {
            // One short sound / beep then stop
            val notificationSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setSound(notificationSound)
                .setVibrate(longArrayOf(0, 250))
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(DEEP_DIVE_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post Deep Dive notification: ${e.message}", e)
        }
    }

    fun dismissNotification(context: Context, notificationId: Int = DEEP_DIVE_NOTIFICATION_ID) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }

    fun formatTimeRemaining(endTimeMillis: Long): String {
        val remainingMillis = endTimeMillis - System.currentTimeMillis()
        if (remainingMillis <= 0) return "Finishing now"
        val minutes = (remainingMillis / (1000 * 60)).toInt()
        val hours = minutes / 60
        val remMinutes = minutes % 60

        return when {
            hours > 0 && remMinutes > 0 -> "$hours hr $remMinutes min left"
            hours > 0 -> "$hours hr left"
            remMinutes > 0 -> "$remMinutes min left"
            else -> "< 1 min left"
        }
    }

    fun formatClockTime(timeMillis: Long): String {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        return formatter.format(Date(timeMillis))
    }

    /**
     * Resolves the exact millisecond timestamp for a custom reminder given [hourOfDay] and [minute],
     * ensuring it occurs strictly in the future relative to [nowMillis] and strictly before [targetEndMillis].
     * Correctly handles AM/PM, today vs tomorrow (e.g. crossing midnight), and returns null if no valid timestamp exists.
     */
    fun resolveCustomReminderMillis(
        hourOfDay: Int,
        minute: Int,
        nowMillis: Long,
        targetEndMillis: Long
    ): Long? {
        if (targetEndMillis <= nowMillis) return null

        // Candidate 1: Today
        val calToday = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Candidate 2: Tomorrow (for sessions crossing midnight)
        val calTomorrow = (calToday.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }

        return when {
            calToday.timeInMillis > nowMillis && calToday.timeInMillis < targetEndMillis -> calToday.timeInMillis
            calTomorrow.timeInMillis > nowMillis && calTomorrow.timeInMillis < targetEndMillis -> calTomorrow.timeInMillis
            else -> null
        }
    }
}
