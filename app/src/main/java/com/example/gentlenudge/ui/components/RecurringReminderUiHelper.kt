package com.example.gentlenudge.ui.components

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class RecurringReminderDisplay(
    val nextOccurrence: LocalDateTime,
    val relativeDateLabel: String,
    val countdown: String,
    val displayString: String
)

object RecurringReminderUiHelper {

    fun isRecurringReminder(repeat: String?): Boolean {
        if (repeat.isNullOrBlank()) return false
        val trimmed = repeat.trim()
        return !trimmed.equals("Does not repeat", ignoreCase = true) && !trimmed.equals("Once", ignoreCase = true)
    }

    fun parseScheduledTime(timeLabel: String): LocalTime? {
        if (timeLabel.isBlank() || timeLabel.equals("any time", ignoreCase = true)) return null

        val clean = timeLabel
            .replace("(?i)morning|afternoon|evening|night|today|tomorrow".toRegex(), "")
            .replace(",", "")
            .trim()

        // 1. Check for 12-hour pattern: e.g. 5:00 AM, 05:00 AM, 5 AM, 5pm, 12:01 am
        val match12 = Regex("(?i)(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)").find(clean)
        if (match12 != null) {
            var hour = match12.groupValues[1].toInt()
            val minute = if (match12.groupValues[2].isNotEmpty()) match12.groupValues[2].toInt() else 0
            val ampm = match12.groupValues[3].lowercase()
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            if (hour in 0..23 && minute in 0..59) {
                return LocalTime.of(hour, minute)
            }
        }

        // 2. Check for 24-hour pattern: e.g. 05:00, 17:30
        val match24 = Regex("(?i)(\\d{1,2}):(\\d{2})").find(clean)
        if (match24 != null) {
            val hour = match24.groupValues[1].toInt()
            val minute = match24.groupValues[2].toInt()
            if (hour in 0..23 && minute in 0..59) {
                return LocalTime.of(hour, minute)
            }
        }

        return null
    }

    fun calculateNextOccurrence(
        now: LocalDateTime,
        scheduledTime: LocalTime
    ): LocalDateTime {
        val todayCandidate = LocalDateTime.of(now.toLocalDate(), scheduledTime)
        return if (todayCandidate.isAfter(now)) {
            todayCandidate
        } else {
            todayCandidate.plusDays(1)
        }
    }

    fun calculateRelativeDateLabel(nowDate: LocalDate, targetDate: LocalDate): String {
        val daysBetween = ChronoUnit.DAYS.between(nowDate, targetDate)
        return when (daysBetween) {
            0L -> "Today"
            1L -> "Tomorrow"
            else -> targetDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    fun calculateCountdown(now: LocalDateTime, nextOccurrence: LocalDateTime): String {
        val duration = Duration.between(now, nextOccurrence)
        val totalSeconds = duration.seconds
        if (totalSeconds <= 0) return "now"

        val totalMinutes = (totalSeconds + 59) / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    fun getRecurringReminderDisplay(
        timeLabel: String,
        repeatRule: String?,
        now: LocalDateTime = LocalDateTime.now(),
        targetOccurrenceMillis: Long? = null
    ): RecurringReminderDisplay? {
        if (!isRecurringReminder(repeatRule)) return null
        val scheduledTime = parseScheduledTime(timeLabel) ?: return null
        val nextOccurrence = if (targetOccurrenceMillis != null && targetOccurrenceMillis > 0L) {
            LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(targetOccurrenceMillis),
                java.time.ZoneId.systemDefault()
            )
        } else {
            calculateNextOccurrence(now, scheduledTime)
        }
        val relativeDate = calculateRelativeDateLabel(now.toLocalDate(), nextOccurrence.toLocalDate())
        val countdown = calculateCountdown(now, nextOccurrence)
        return RecurringReminderDisplay(
            nextOccurrence = nextOccurrence,
            relativeDateLabel = relativeDate,
            countdown = countdown,
            displayString = "$relativeDate, $countdown"
        )
    }

    fun getRecurringReminderDisplay(
        task: com.example.gentlenudge.data.model.NudgeTask,
        context: android.content.Context? = null,
        now: LocalDateTime = LocalDateTime.now(),
        nowMillis: Long = System.currentTimeMillis()
    ): RecurringReminderDisplay? {
        if (!isRecurringReminder(task.repeat)) return null
        val targetOccurrenceMillis = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(task, context, nowMillis)
        return getRecurringReminderDisplay(
            timeLabel = task.timeLabel,
            repeatRule = task.repeat,
            now = now,
            targetOccurrenceMillis = targetOccurrenceMillis
        )
    }
}
