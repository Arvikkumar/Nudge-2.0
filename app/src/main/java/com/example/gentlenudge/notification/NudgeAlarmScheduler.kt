package com.example.gentlenudge.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.gentlenudge.data.model.NudgeTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object NudgeAlarmScheduler {

    private const val TAG = "NudgeAlarmScheduler"
    private const val PREFS_NAME = "gentle_nudge_prefs"
    const val KEY_DELIVERED_TASK_REMINDERS = "delivered_task_reminders"
    val deliveryLock = Any()

    /**
     * Determines whether a task has a recurring rule or is a one-time reminder.
     */
    fun isTaskRepeating(task: NudgeTask): Boolean {
        val rule = task.repeat.trim()
        return rule.isNotBlank() &&
                !rule.equals("Does not repeat", ignoreCase = true) &&
                !rule.equals("Once", ignoreCase = true)
    }

    /**
     * Creates a robust unique key for a task occurrence (e.g. "task_42_1788883200000").
     */
    fun getTaskOccurrenceKey(taskId: Long, scheduledMillis: Long): String {
        return "task_${taskId}_${scheduledMillis}"
    }

    /**
     * Checks if a specific task occurrence was already delivered.
     */
    fun isTaskReminderDelivered(context: Context, occurrenceKey: String): Boolean {
        synchronized(deliveryLock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val delivered = prefs.getStringSet(KEY_DELIVERED_TASK_REMINDERS, emptySet()) ?: emptySet()
            return delivered.contains(occurrenceKey)
        }
    }

    /**
     * Atomically claims delivery for a specific task occurrence.
     * Returns true if successfully claimed (i.e. was not previously delivered).
     * Returns false if this occurrence was already claimed or delivered.
     */
    fun tryClaimTaskReminderDelivery(context: Context, occurrenceKey: String): Boolean {
        synchronized(deliveryLock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val delivered = HashSet(prefs.getStringSet(KEY_DELIVERED_TASK_REMINDERS, emptySet()) ?: emptySet())
            if (delivered.contains(occurrenceKey)) {
                return false
            }
            delivered.add(occurrenceKey)
            if (delivered.size > 1000) {
                val list = delivered.toList()
                val pruned = list.takeLast(500).toSet()
                prefs.edit().putStringSet(KEY_DELIVERED_TASK_REMINDERS, pruned).commit()
            } else {
                prefs.edit().putStringSet(KEY_DELIVERED_TASK_REMINDERS, delivered).commit()
            }
            return true
        }
    }

    fun markTaskReminderDelivered(context: Context, occurrenceKey: String) {
        tryClaimTaskReminderDelivery(context, occurrenceKey)
    }

    fun unclaimTaskReminderDelivery(context: Context, occurrenceKey: String) {
        synchronized(deliveryLock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val delivered = HashSet(prefs.getStringSet(KEY_DELIVERED_TASK_REMINDERS, emptySet()) ?: emptySet())
            if (delivered.remove(occurrenceKey)) {
                prefs.edit().putStringSet(KEY_DELIVERED_TASK_REMINDERS, delivered).commit()
            }
        }
    }

    /**
     * Gets the persisted scheduled trigger millis for this task, or calculates and establishes it once.
     */
    fun getScheduledTriggerMillis(context: Context, task: NudgeTask, now: Long = System.currentTimeMillis()): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getLong("task_scheduled_trigger_${task.id}", -1L)
        if (saved > 0L) {
            return saved
        }
        val baseRef = if (task.createdAt > 0L) task.createdAt else now
        val calculated = calculateTriggerMillis(task.dateLabel, task.timeLabel, baseRef)
        if (calculated > 0L) {
            saveScheduledTriggerMillis(context, task.id, calculated)
        }
        return calculated
    }

    fun saveScheduledTriggerMillis(context: Context, taskId: Long, triggerMillis: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong("task_scheduled_trigger_${taskId}", triggerMillis).apply()
    }

    /**
     * Checks whether a time label represents a relative phrase like "1 minute later", "in 2 mins", etc.
     */
    fun isRelativeTimePhrase(timeLabel: String): Boolean {
        val clean = timeLabel.trim()
        val relativeRegex = Regex("(?i)^(?:in\\s+)?(\\d+)\\s*(minutes?|mins?|hours?|hrs?|seconds?|secs?)\\s*(?:later)?$")
        return relativeRegex.matches(clean)
    }

    /**
     * Calculates the next chronological trigger epoch millis for a task.
     * For relative phrases (e.g. "in 2 minutes"), it evaluates relative to now.
     * For fixed clock-times:
     * - If scheduled time is in the future, returns that time.
     * - If scheduled time is in the past:
     *     - If task repeats (e.g. "Every day", "Daily", "Weekly", "Weekdays"), advances along the repeat rule until strictly > now.
     *     - If non-repeating, returns the base trigger without rolling over to tomorrow.
     *     - Never falls back to now + 2000L for fixed clock times.
     */
    fun calculateNextOccurrenceMillis(
        dateLabel: String,
        timeLabel: String,
        repeatRule: String? = null,
        now: Long = System.currentTimeMillis(),
        baseRef: Long = now
    ): Long {
        val baseTrigger = calculateTriggerMillis(dateLabel, timeLabel, baseRef)
        if (isRelativeTimePhrase(timeLabel)) {
            return baseTrigger
        }

        if (baseTrigger > now) {
            return baseTrigger
        }

        val rule = repeatRule?.trim().orEmpty()
        val isRepeating = rule.isNotBlank() &&
                !rule.equals("Does not repeat", ignoreCase = true) &&
                !rule.equals("Once", ignoreCase = true)

        if (isRepeating) {
            val cal = Calendar.getInstance().apply { timeInMillis = baseTrigger }
            var attempts = 0
            while (cal.timeInMillis <= now && attempts < 1000) {
                val advanced = NudgeNotificationReceiver.calculateNextCalendar(cal, rule) ?: break
                cal.timeInMillis = advanced.timeInMillis
                attempts++
            }
            if (cal.timeInMillis > now) {
                return cal.timeInMillis
            }
        }

        // For non-repeating tasks, do NOT roll over to tomorrow!
        // A one-time reminder remains tied to its exact scheduled time.
        return baseTrigger
    }

    fun scheduleTask(context: Context, task: NudgeTask, forceRecalculate: Boolean = false) {
        if (task.isDone || task.isDeleted) return

        val now = System.currentTimeMillis()
        val isRepeating = isTaskRepeating(task)

        val finalTriggerMillis: Long

        if (isRepeating) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getLong("task_scheduled_trigger_${task.id}", -1L)

            val trigger = if (!forceRecalculate && saved > 0L) {
                val occurrenceKey = getTaskOccurrenceKey(task.id, saved)
                val isDelivered = isTaskReminderDelivered(context, occurrenceKey)

                if (saved > now) {
                    saved
                } else if (!isDelivered && !task.isDone) {
                    // Scheduled occurrence has reached or passed its time, but has NOT been delivered or completed.
                    // DO NOT silently advance to tomorrow!
                    // Preserve the saved occurrence as the authoritative trigger.
                    saved
                } else {
                    // Occurrence was delivered or completed, advance from saved occurrence along repeat rule
                    val cal = Calendar.getInstance().apply { timeInMillis = saved }
                    var attempts = 0
                    while (cal.timeInMillis <= now && attempts < 1000) {
                        val advanced = NudgeNotificationReceiver.calculateNextCalendar(cal, task.repeat.trim()) ?: break
                        cal.timeInMillis = advanced.timeInMillis
                        attempts++
                    }
                    if (cal.timeInMillis > now) {
                        saveScheduledTriggerMillis(context, task.id, cal.timeInMillis)
                        cal.timeInMillis
                    } else {
                        val targetMillis = calculateNextOccurrenceMillis(task.dateLabel, task.timeLabel, task.repeat, now)
                        saveScheduledTriggerMillis(context, task.id, targetMillis)
                        targetMillis
                    }
                }
            } else {
                val baseRef = if (forceRecalculate || task.createdAt <= 0L) now else task.createdAt
                val targetMillis = calculateNextOccurrenceMillis(task.dateLabel, task.timeLabel, task.repeat, now, baseRef)
                val resolved = if (isRelativeTimePhrase(task.timeLabel) && targetMillis <= now) {
                    now + 2000L
                } else {
                    targetMillis
                }
                saveScheduledTriggerMillis(context, task.id, resolved)
                resolved
            }

            finalTriggerMillis = trigger
        } else {
            // One-time / non-repeating task:
            // An immutable scheduled trigger is established once (at creation or explicit edit/reschedule)
            // and must NOT be recalculated relative to the current date across midnight, reboots, or date changes.
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getLong("task_scheduled_trigger_${task.id}", -1L)

            val trigger = if (!forceRecalculate && saved > 0L) {
                saved
            } else {
                // Initial creation, explicit user edit/reschedule, or legacy task
                val baseRef = if (forceRecalculate || task.createdAt <= 0L) now else task.createdAt
                val calculated = calculateTriggerMillis(task.dateLabel, task.timeLabel, baseRef)
                val resolved = if (isRelativeTimePhrase(task.timeLabel) && calculated <= now) {
                    now + 2000L
                } else {
                    calculated
                }
                saveScheduledTriggerMillis(context, task.id, resolved)
                resolved
            }

            finalTriggerMillis = trigger

            // For one-time tasks: if this occurrence was already delivered, do not schedule again
            val occurrenceKey = getTaskOccurrenceKey(task.id, finalTriggerMillis)
            if (isTaskReminderDelivered(context, occurrenceKey)) {
                Log.d(TAG, "Task ${task.id} occurrence $occurrenceKey already delivered. Skipping schedule.")
                return
            }

            // If a non-repeating task is in the past, do not schedule alarm and do NOT roll over
            if (finalTriggerMillis <= now) {
                Log.w(TAG, "Task ${task.id} ('${task.title}') is in the past ($finalTriggerMillis <= $now) and non-repeating. Skipping schedule.")
                return
            }
        }

        Log.d(TAG, "Scheduling task ${task.id} ('${task.title}'): date='${task.dateLabel}', time='${task.timeLabel}', repeat='${task.repeat}', now=$now, targetMillis=$finalTriggerMillis (in ${(finalTriggerMillis - now) / 1000}s)")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_FIRE_NUDGE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_TITLE, task.title)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_CATEGORY, task.category)
            putExtra(NudgeNotificationHelper.EXTRA_TASK_SCHEDULED_MILLIS, finalTriggerMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerMillis, pendingIntent)
                    Log.d(TAG, "Scheduled alarm with setAndAllowWhileIdle (exact alarm not permitted) for task ${task.id}")
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerMillis, pendingIntent)
                    Log.d(TAG, "Scheduled alarm with setExactAndAllowWhileIdle for task ${task.id} at $finalTriggerMillis")
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, finalTriggerMillis, pendingIntent)
                Log.d(TAG, "Scheduled alarm with setExact for task ${task.id} at $finalTriggerMillis")
            }
            Log.d(TAG, "Successfully scheduled alarm for task ${task.id} ('${task.title}') at $finalTriggerMillis (in ${(finalTriggerMillis - now) / 1000}s)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission denied, falling back to setAndAllowWhileIdle: ${e.message}")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, finalTriggerMillis, pendingIntent)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Failed fallback alarm: ${e2.message}", e2)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm: ${e.message}", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, finalTriggerMillis, pendingIntent)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Failed final fallback alarm: ${e2.message}", e2)
            }
        }
    }

    fun cancelTask(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NudgeNotificationReceiver::class.java).apply {
            action = NudgeNotificationHelper.ACTION_FIRE_NUDGE
            putExtra(NudgeNotificationHelper.EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            NudgeNotificationHelper.dismissNotification(context, taskId)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove("task_scheduled_trigger_${taskId}").apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling alarm: ${e.message}", e)
        }
    }

    fun calculateTriggerMillis(dateLabel: String, timeLabel: String, now: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }

        // First check for relative time phrases like "1 minute later", "2 minutes later", "in 1 minute", "in 2 mins", "30 minutes later", "1 hour later"
        val relativeMatch = Regex("(?i)^(?:in\\s+)?(\\d+)\\s*(minutes?|mins?|hours?|hrs?|seconds?|secs?)\\s*(?:later)?$").find(timeLabel.trim())
        if (relativeMatch != null) {
            val amount = relativeMatch.groupValues[1].toIntOrNull() ?: 1
            val unit = relativeMatch.groupValues[2].lowercase()
            val millisToAdd = when {
                unit.startsWith("second") || unit.startsWith("sec") -> amount * 1000L
                unit.startsWith("minute") || unit.startsWith("min") -> amount * 60 * 1000L
                unit.startsWith("hour") || unit.startsWith("hr") -> amount * 60 * 60 * 1000L
                else -> amount * 60 * 1000L
            }
            return now + millisToAdd
        }

        // Parse Date component
        when (dateLabel.lowercase().trim()) {
            "today", "tonight" -> {
                // Today and Tonight remain on current date
            }
            "tomorrow" -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            "this weekend" -> {
                // Advance to Saturday
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val daysUntilSaturday = (Calendar.SATURDAY - dayOfWeek + 7) % 7
                calendar.add(Calendar.DAY_OF_YEAR, if (daysUntilSaturday == 0) 7 else daysUntilSaturday)
            }
            "next week" -> {
                // Advance to next Monday
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val daysUntilMonday = (Calendar.MONDAY - dayOfWeek + 7) % 7
                calendar.add(Calendar.DAY_OF_YEAR, if (daysUntilMonday == 0) 7 else daysUntilMonday)
            }
            else -> {
                // Try parsing custom date or weekday (e.g. "Friday", "Aug 25", "Aug 25, 2026")
                tryParseDate(calendar, dateLabel)
            }
        }

        // Parse Time component
        val parsedHourMinute = parseTimeLabel(timeLabel)
        if (parsedHourMinute != null) {
            calendar.set(Calendar.HOUR_OF_DAY, parsedHourMinute.first)
            calendar.set(Calendar.MINUTE, parsedHourMinute.second)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            if (dateLabel.equals("Tonight", ignoreCase = true) && calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // Default time if unspecified or "Any time"
            if (dateLabel.equals("Tonight", ignoreCase = true)) {
                calendar.set(Calendar.HOUR_OF_DAY, 20)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.MINUTE, 30)
                }
                if (calendar.timeInMillis <= now) {
                    // Normal Tonight cutoff (8:30 PM) has passed. Advance to next valid occurrence (tomorrow night at 8:00 PM)
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 20)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                }
            } else if (dateLabel.equals("Today", ignoreCase = true)) {
                // If it's today and no time specified, default to 1 hour from now
                calendar.timeInMillis = now + (60 * 60 * 1000L)
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, 9)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        }

        return calendar.timeInMillis
    }

    private fun tryParseDate(calendar: Calendar, text: String) {
        val clean = text.trim()
        val days = mapOf(
            "sunday" to Calendar.SUNDAY,
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY
        )

        // Check for "Next [Day]", "This [Day]", or "[Day]"
        val dayPattern = Regex("(?i)\\b(?:(this|next)\\s+)?(sunday|monday|tuesday|wednesday|thursday|friday|saturday)\\b")
        val match = dayPattern.find(clean)
        if (match != null) {
            val modifier = match.groupValues[1].lowercase()
            val dayName = match.groupValues[2].lowercase()
            val targetDay = days[dayName]
            if (targetDay != null) {
                val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                var diff = targetDay - currentDay
                if (modifier == "next") {
                    if (diff <= 0) diff += 7
                    diff += 7
                } else {
                    if (diff <= 0) diff += 7
                }
                calendar.add(Calendar.DAY_OF_YEAR, diff)
                return
            }
        }

        val targetDay = days[clean.lowercase()]
        if (targetDay != null) {
            val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
            var diff = targetDay - currentDay
            if (diff <= 0) diff += 7
            calendar.add(Calendar.DAY_OF_YEAR, diff)
            return
        }

        val dateFormats = listOf(
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()),
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()),
            SimpleDateFormat("MMM d", Locale.getDefault()),
            SimpleDateFormat("MMMM d", Locale.getDefault()),
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()),
            SimpleDateFormat("d MMMM yyyy", Locale.getDefault()),
            SimpleDateFormat("d MMM", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("MMM d, yyyy", Locale.US),
            SimpleDateFormat("MMM d", Locale.US),
            SimpleDateFormat("d MMM yyyy", Locale.US),
            SimpleDateFormat("d MMM", Locale.US)
        )

        val currentYear = calendar.get(Calendar.YEAR)
        val now = System.currentTimeMillis()

        for (format in dateFormats) {
            try {
                val parsedDate = format.parse(clean)
                if (parsedDate != null) {
                    val tempCal = Calendar.getInstance().apply { time = parsedDate }
                    calendar.set(Calendar.MONTH, tempCal.get(Calendar.MONTH))
                    calendar.set(Calendar.DAY_OF_MONTH, tempCal.get(Calendar.DAY_OF_MONTH))

                    if (clean.contains(Regex("\\b20\\d{2}\\b"))) {
                        calendar.set(Calendar.YEAR, tempCal.get(Calendar.YEAR))
                    } else {
                        calendar.set(Calendar.YEAR, currentYear)
                        val testCal = calendar.clone() as Calendar
                        testCal.set(Calendar.HOUR_OF_DAY, 23)
                        testCal.set(Calendar.MINUTE, 59)
                        if (testCal.timeInMillis < now) {
                            calendar.add(Calendar.YEAR, 1)
                        }
                    }
                    return
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun parseTimeLabel(timeLabel: String): Pair<Int, Int>? {
        if (timeLabel.isBlank() || timeLabel.equals("any time", ignoreCase = true)) return null

        val clean = timeLabel.replace("(?i)morning|afternoon|evening|night".toRegex(), "").trim()
        val timePatterns = listOf(
            SimpleDateFormat("h:mm a", Locale.US),
            SimpleDateFormat("hh:mm a", Locale.US),
            SimpleDateFormat("h a", Locale.US),
            SimpleDateFormat("H:mm", Locale.US),
            SimpleDateFormat("HH:mm", Locale.US)
        )

        for (pattern in timePatterns) {
            try {
                val date = pattern.parse(clean)
                if (date != null) {
                    val cal = Calendar.getInstance().apply { time = date }
                    return Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                }
            } catch (e: Exception) {
                // Continue trying next pattern
            }
        }

        // Regex fallback for "10:00 AM", "6:00 PM", "9am", "7pm"
        val regex = "(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(clean)
        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val ampm = match.groupValues[3].lowercase()

            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            return Pair(hour, minute)
        }

        return null
    }

    fun calculateSnoozeMillis(snoozeDuration: String): Long {
        val now = System.currentTimeMillis()
        return when (snoozeDuration.lowercase().trim()) {
            "15 minutes" -> now + (15 * 60 * 1000L)
            "30 minutes" -> now + (30 * 60 * 1000L)
            "1 hour" -> now + (60 * 60 * 1000L)
            "tonight" -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 20)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                if (cal.timeInMillis <= now) now + (60 * 60 * 1000L) else cal.timeInMillis
            }
            "tomorrow" -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                cal.timeInMillis
            }
            else -> now + (30 * 60 * 1000L)
        }
    }
}
