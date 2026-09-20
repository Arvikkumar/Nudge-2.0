package com.example.gentlenudge.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.gentlenudge.data.events.NudgeEventsRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class EventNotificationMode(val displayName: String, val defaultDays: Int) {
    OFF("Off", 0),
    ONE_DAY_BEFORE("1 day before", 1),
    TWO_DAYS_BEFORE("2 days before", 2),
    CUSTOM("Custom", 3);

    companion object {
        fun fromString(value: String?): EventNotificationMode {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: OFF
        }
    }
}

object NudgeEventNotificationScheduler {

    private const val TAG = "EventNotificationSched"
    private const val PREFS_NAME = "gentle_nudge_prefs"
    const val KEY_EVENT_NOTIF_MODE = "event_notification_mode"
    const val KEY_EVENT_NOTIF_DAYS = "event_notification_days"
    const val KEY_CUSTOM_EVENT_NOTIF_DAYS = "custom_event_notification_days"
    const val KEY_EVENT_NOTIF_HOUR = "event_notification_hour"
    const val KEY_EVENT_NOTIF_MINUTE = "event_notification_minute"
    const val KEY_DELIVERED_EVENT_REMINDERS = "delivered_event_reminders"

    const val DEFAULT_NOTIFICATION_HOUR = 9
    const val DEFAULT_NOTIFICATION_MINUTE = 0

    // Base request code offset to avoid any collisions with task alarms (tasks use IDs e.g. 1..100000)
    private const val EVENT_ALARM_REQUEST_CODE_BASE = 500000

    val deliveryLock = Any()

    fun getEventReminderKey(eventId: String, year: Int, month: Int, day: Int): String {
        return "$eventId-$year-$month-$day"
    }

    fun isEventReminderDelivered(context: Context, reminderKey: String): Boolean {
        synchronized(deliveryLock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val delivered = prefs.getStringSet(KEY_DELIVERED_EVENT_REMINDERS, emptySet()) ?: emptySet()
            return delivered.contains(reminderKey)
        }
    }

    fun markEventReminderDelivered(context: Context, reminderKey: String) {
        synchronized(deliveryLock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val delivered = HashSet(prefs.getStringSet(KEY_DELIVERED_EVENT_REMINDERS, emptySet()) ?: emptySet())
            if (!delivered.contains(reminderKey)) {
                delivered.add(reminderKey)
                prefs.edit().putStringSet(KEY_DELIVERED_EVENT_REMINDERS, delivered).apply()
            }
        }
    }

    fun clearDeliveredEventReminders(context: Context) {
        synchronized(deliveryLock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_DELIVERED_EVENT_REMINDERS).apply()
        }
    }

    fun deliverMissedReminder(
        context: Context,
        eventId: String,
        eventName: String,
        category: String,
        description: String,
        year: Int,
        month: Int,
        day: Int,
        daysBefore: Int
    ): Boolean {
        val reminderKey = getEventReminderKey(eventId, year, month, day)
        synchronized(deliveryLock) {
            if (isEventReminderDelivered(context, reminderKey)) {
                return false
            }

            val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            val eventCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
            }
            val dateLabel = dateFormat.format(eventCal.time)

            val shown = NudgeNotificationHelper.showEventNotification(
                context = context,
                eventId = eventId,
                eventName = eventName,
                category = category,
                description = description,
                dateLabel = dateLabel,
                daysBefore = daysBefore,
                eventYear = year,
                eventMonth = month,
                eventDay = day
            )

            if (shown) {
                markEventReminderDelivered(context, reminderKey)
            }
            return shown
        }
    }

    fun getEventNotificationMode(context: Context): EventNotificationMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_EVENT_NOTIF_MODE, EventNotificationMode.OFF.name)
        return EventNotificationMode.fromString(modeStr)
    }

    fun getEventNotificationDays(context: Context): Int {
        val mode = getEventNotificationMode(context)
        return when (mode) {
            EventNotificationMode.OFF -> 0
            EventNotificationMode.ONE_DAY_BEFORE -> 1
            EventNotificationMode.TWO_DAYS_BEFORE -> 2
            EventNotificationMode.CUSTOM -> getCustomNotificationDays(context)
        }
    }

    fun getCustomNotificationDays(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_CUSTOM_EVENT_NOTIF_DAYS)) {
            val custom = prefs.getInt(KEY_CUSTOM_EVENT_NOTIF_DAYS, 3)
            return if (custom in 1..30) custom else 3
        }
        val mode = getEventNotificationMode(context)
        if (mode == EventNotificationMode.CUSTOM) {
            val legacy = prefs.getInt(KEY_EVENT_NOTIF_DAYS, 3)
            if (legacy in 1..30) return legacy
        }
        return 3
    }

    fun getEventNotificationHour(context: Context): Int {
        val mode = getEventNotificationMode(context)
        return if (mode == EventNotificationMode.CUSTOM) {
            getCustomNotificationHour(context)
        } else {
            DEFAULT_NOTIFICATION_HOUR
        }
    }

    fun getEventNotificationMinute(context: Context): Int {
        val mode = getEventNotificationMode(context)
        return if (mode == EventNotificationMode.CUSTOM) {
            getCustomNotificationMinute(context)
        } else {
            DEFAULT_NOTIFICATION_MINUTE
        }
    }

    fun getCustomNotificationHour(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hour = prefs.getInt(KEY_EVENT_NOTIF_HOUR, DEFAULT_NOTIFICATION_HOUR)
        return if (hour in 0..23) hour else DEFAULT_NOTIFICATION_HOUR
    }

    fun getCustomNotificationMinute(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val min = prefs.getInt(KEY_EVENT_NOTIF_MINUTE, DEFAULT_NOTIFICATION_MINUTE)
        return if (min in 0..59) min else DEFAULT_NOTIFICATION_MINUTE
    }

    fun formatTime(hour: Int, minute: Int): String {
        val isPm = hour >= 12
        val h12 = when (val h = hour % 12) {
            0 -> 12
            else -> h
        }
        val minStr = if (minute < 10) "0$minute" else "$minute"
        val amPm = if (isPm) "PM" else "AM"
        return "$h12:$minStr $amPm"
    }

    fun setEventNotificationSetting(
        context: Context,
        mode: EventNotificationMode,
        customDays: Int = 3,
        customHour: Int = DEFAULT_NOTIFICATION_HOUR,
        customMinute: Int = DEFAULT_NOTIFICATION_MINUTE
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val safeCustomDays = if (customDays in 1..30) customDays else 3
        val safeCustomHour = if (customHour in 0..23) customHour else DEFAULT_NOTIFICATION_HOUR
        val safeCustomMinute = if (customMinute in 0..59) customMinute else DEFAULT_NOTIFICATION_MINUTE

        val finalDays = when (mode) {
            EventNotificationMode.OFF -> 0
            EventNotificationMode.ONE_DAY_BEFORE -> 1
            EventNotificationMode.TWO_DAYS_BEFORE -> 2
            EventNotificationMode.CUSTOM -> safeCustomDays
        }

        val editor = prefs.edit()
            .putString(KEY_EVENT_NOTIF_MODE, mode.name)
            .putInt(KEY_EVENT_NOTIF_DAYS, finalDays)
            .putInt(KEY_CUSTOM_EVENT_NOTIF_DAYS, safeCustomDays)

        if (mode == EventNotificationMode.CUSTOM) {
            editor.putInt(KEY_EVENT_NOTIF_HOUR, safeCustomHour)
            editor.putInt(KEY_EVENT_NOTIF_MINUTE, safeCustomMinute)
        }

        editor.apply()

        // Always cancel previous alarms first to avoid duplicate or orphaned schedules
        cancelAllEventNotifications(context)

        if (mode != EventNotificationMode.OFF) {
            val targetHour = if (mode == EventNotificationMode.CUSTOM) safeCustomHour else DEFAULT_NOTIFICATION_HOUR
            val targetMinute = if (mode == EventNotificationMode.CUSTOM) safeCustomMinute else DEFAULT_NOTIFICATION_MINUTE
            scheduleAllEventNotifications(
                context = context,
                daysBefore = finalDays,
                notifHour = targetHour,
                notifMinute = targetMinute
            )
        }
    }

    fun rescheduleIfEnabled(context: Context) {
        val mode = getEventNotificationMode(context)
        if (mode != EventNotificationMode.OFF) {
            val days = getEventNotificationDays(context)
            val hour = getEventNotificationHour(context)
            val minute = getEventNotificationMinute(context)
            scheduleAllEventNotifications(context, days, hour, minute)
        } else {
            cancelAllEventNotifications(context)
        }
    }

    fun scheduleAllEventNotifications(
        context: Context,
        daysBefore: Int = getEventNotificationDays(context),
        notifHour: Int = getEventNotificationHour(context),
        notifMinute: Int = getEventNotificationMinute(context),
        lookaheadDays: Int = 90
    ) {
        if (daysBefore <= 0) {
            cancelAllEventNotifications(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nowMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

        for (dayOffset in 0..lookaheadDays) {
            val eventDateCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            val year = eventDateCal.get(Calendar.YEAR)
            val month = eventDateCal.get(Calendar.MONTH) + 1
            val day = eventDateCal.get(Calendar.DAY_OF_MONTH)

            // Calculate the end of this event day (23:59:59.999) in local time
            val endOfEventDayCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endOfEventDayMillis = endOfEventDayCal.timeInMillis

            val events = NudgeEventsRepository.getEventsForDate(year, month, day)
            for (event in events) {
                val reminderKey = getEventReminderKey(event.id, year, month, day)

                // If already delivered, do not show or schedule again (Duplicate Protection)
                if (isEventReminderDelivered(context, reminderKey)) {
                    continue
                }

                // Calculate reminder trigger time: (eventDate - daysBefore) at scheduled notification time
                val reminderCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, day)
                    add(Calendar.DAY_OF_YEAR, -daysBefore)
                    set(Calendar.HOUR_OF_DAY, notifHour)
                    set(Calendar.MINUTE, notifMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val triggerMillis = reminderCal.timeInMillis

                if (triggerMillis > nowMillis) {
                    // Future reminder -> schedule alarm with AlarmManager
                    val requestCode = getEventRequestCode(event.id, year, month, day)
                    val dateLabel = dateFormat.format(eventDateCal.time)

                    val intent = Intent(context, NudgeNotificationReceiver::class.java).apply {
                        action = NudgeNotificationHelper.ACTION_FIRE_EVENT_NOTIFICATION
                        putExtra(NudgeNotificationHelper.EXTRA_EVENT_ID, event.id)
                        putExtra(NudgeNotificationHelper.EXTRA_EVENT_NAME, event.name)
                        putExtra(NudgeNotificationHelper.EXTRA_EVENT_CATEGORY, event.category.displayName)
                        putExtra(NudgeNotificationHelper.EXTRA_EVENT_DESCRIPTION, event.description)
                        putExtra(NudgeNotificationHelper.EXTRA_EVENT_DATE_LABEL, dateLabel)
                        putExtra(NudgeNotificationHelper.EXTRA_DAYS_BEFORE, daysBefore)
                        putExtra(NudgeNotificationHelper.EXTRA_EVENT_YEAR, year)
                        putExtra(NudgeNotificationHelper.EXTRA_EVENT_MONTH, month)
                        putExtra(NudgeNotificationHelper.EXTRA_EVENT_DAY, day)
                        putExtra(NudgeNotificationHelper.EXTRA_REMINDER_KEY, reminderKey)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                            } else {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                            }
                        } else {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                        }
                    } catch (e: Exception) {
                        try {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                        } catch (e2: Exception) {
                            Log.e(TAG, "Failed to schedule event alarm for ${event.name}: ${e2.message}")
                        }
                    }
                } else {
                    // Reminder time has already passed!
                    // Check if it is still within the valid recovery window (event day has not completely passed)
                    if (nowMillis <= endOfEventDayMillis) {
                        Log.i(TAG, "Recovering missed event reminder: ${event.name} (trigger: $triggerMillis, now: $nowMillis)")
                        deliverMissedReminder(
                            context = context,
                            eventId = event.id,
                            eventName = event.name,
                            category = event.category.displayName,
                            description = event.description,
                            year = year,
                            month = month,
                            day = day,
                            daysBefore = daysBefore
                        )
                    } else {
                        // Event day has completely ended; mark handled so it won't be processed again
                        markEventReminderDelivered(context, reminderKey)
                    }
                }
            }
        }
    }

    fun cancelAllEventNotifications(context: Context, lookaheadDays: Int = 90) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nowMillis = System.currentTimeMillis()

        for (dayOffset in 0..lookaheadDays) {
            val eventDateCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val year = eventDateCal.get(Calendar.YEAR)
            val month = eventDateCal.get(Calendar.MONTH) + 1
            val day = eventDateCal.get(Calendar.DAY_OF_MONTH)

            val events = NudgeEventsRepository.getEventsForDate(year, month, day)
            for (event in events) {
                val requestCode = getEventRequestCode(event.id, year, month, day)
                val intent = Intent(context, NudgeNotificationReceiver::class.java).apply {
                    action = NudgeNotificationHelper.ACTION_FIRE_EVENT_NOTIFICATION
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    try {
                        alarmManager.cancel(pendingIntent)
                        pendingIntent.cancel()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to cancel event alarm: ${e.message}")
                    }
                }
            }
        }
    }

    fun getEventRequestCode(eventId: String, year: Int, month: Int, day: Int): Int {
        val key = "$eventId-$year-$month-$day"
        return (Math.abs(key.hashCode()) % 400000) + EVENT_ALARM_REQUEST_CODE_BASE
    }
}
