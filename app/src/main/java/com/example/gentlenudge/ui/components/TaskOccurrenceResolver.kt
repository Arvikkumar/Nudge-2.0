package com.example.gentlenudge.ui.components

import android.content.Context
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.notification.NudgeAlarmScheduler
import com.example.gentlenudge.notification.NudgeNotificationReceiver
import java.util.Calendar

object TaskOccurrenceResolver {

    private const val PREFS_NAME = "gentle_nudge_prefs"

    /**
     * Resolves the authoritative target occurrence epoch timestamp for a task in the local timezone.
     *
     * A. ONE-TIME REMINDER:
     * - Uses the persisted absolute scheduled trigger from SharedPreferences (task_scheduled_trigger_${task.id}) if available.
     * - Fallback: If not found in SharedPreferences, uses task.createdAt (or nowMillis if createdAt <= 0) as baseRef
     *   for NudgeAlarmScheduler.calculateTriggerMillis(task.dateLabel, task.timeLabel, baseRef).
     *   Crucially, for non-repeating tasks, once scheduled or created, this occurrence DOES NOT roll forward to tomorrow
     *   when the calendar date changes or when the time passes.
     *
     * B. RECURRING REMINDER:
     * - Uses the existing recurring calculation (NudgeAlarmScheduler.calculateNextOccurrenceMillis / RecurringReminderUiHelper)
     *   to calculate the next legitimate occurrence.
     */
    fun resolveTargetOccurrenceMillis(
        task: NudgeTask,
        context: Context? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val isRepeating = RecurringReminderUiHelper.isRecurringReminder(task.repeat)

        if (isRepeating) {
            // 1. Check persisted scheduled trigger first if available
            if (context != null && task.id > 0L) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val saved = prefs.getLong("task_scheduled_trigger_${task.id}", -1L)
                if (saved > 0L) {
                    val occurrenceKey = NudgeAlarmScheduler.getTaskOccurrenceKey(task.id, saved)
                    val isDelivered = NudgeAlarmScheduler.isTaskReminderDelivered(context, occurrenceKey)

                    if (saved > nowMillis) {
                        return saved
                    }

                    if (!isDelivered && !task.isDone) {
                        // Scheduled occurrence has reached or passed its time, but has NOT been delivered or completed.
                        // It remains the active (missed / due) occurrence!
                        return saved
                    }

                    val cal = Calendar.getInstance().apply { timeInMillis = saved }
                    var attempts = 0
                    while (cal.timeInMillis <= nowMillis && attempts < 1000) {
                        val advanced = NudgeNotificationReceiver.calculateNextCalendar(cal, task.repeat.trim()) ?: break
                        cal.timeInMillis = advanced.timeInMillis
                        attempts++
                    }
                    return cal.timeInMillis
                }
            }

            // 2. Fallback when context is null or not yet saved:
            val isRelativeFromCreation = task.dateLabel.equals("Tomorrow", ignoreCase = true) ||
                task.dateLabel.equals("This weekend", ignoreCase = true) ||
                task.dateLabel.equals("Next week", ignoreCase = true)
            val baseRef = if (isRelativeFromCreation && task.createdAt > 0L) task.createdAt else nowMillis
            return NudgeAlarmScheduler.calculateNextOccurrenceMillis(
                dateLabel = task.dateLabel,
                timeLabel = task.timeLabel,
                repeatRule = task.repeat,
                now = nowMillis,
                baseRef = baseRef
            )
        }

        // ONE-TIME REMINDER:
        // 1. Check persisted scheduled trigger first (established at alarm scheduling time)
        if (context != null && task.id > 0L) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getLong("task_scheduled_trigger_${task.id}", -1L)
            if (saved > 0L) {
                return saved
            }
        }

        // 2. Fallback when not yet persisted or context unavailable:
        // Use createdAt as the fixed anchor so relative labels like "Tomorrow" evaluated after creation
        // remain relative to the moment the task was created.
        val isRelativeFromCreation = task.dateLabel.equals("Tomorrow", ignoreCase = true) ||
            task.dateLabel.equals("This weekend", ignoreCase = true) ||
            task.dateLabel.equals("Next week", ignoreCase = true)
        val baseRef = if (isRelativeFromCreation && task.createdAt > 0L) task.createdAt else nowMillis
        val calculated = NudgeAlarmScheduler.calculateTriggerMillis(task.dateLabel, task.timeLabel, baseRef)

        // Cache into SharedPreferences if context is available and task has valid id
        if (context != null && task.id > 0L && calculated > 0L) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong("task_scheduled_trigger_${task.id}", calculated).apply()
            } catch (_: Exception) {}
        }

        return calculated
    }

    /**
     * Checks if the occurrence falls on the same calendar day as nowMillis in the local timezone.
     */
    fun isOccurrenceToday(
        occurrenceMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val targetCal = Calendar.getInstance().apply { timeInMillis = occurrenceMillis }
        return nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Checks if the occurrence is today OR in the past relative to the start of today in the local timezone.
     * Pending tasks whose scheduled occurrence was today or earlier belong in Today's section.
     */
    fun isOccurrenceTodayOrPast(
        occurrenceMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val startOfToday = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfToday = Calendar.getInstance().apply {
            timeInMillis = startOfToday.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return occurrenceMillis < endOfToday.timeInMillis
    }

    /**
     * Calculates the epoch millisecond of the next upcoming midnight (00:00:00.000 of tomorrow) in local timezone.
     */
    fun getNextMidnightMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    /**
     * Checks if two epoch timestamps represent the same calendar day in the local timezone.
     */
    fun isSameDay(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
