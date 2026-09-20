package com.example.gentlenudge.data.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class DayCompletionState {
    COMPLETED,    // Actual >= Target (✓)
    PARTIAL,      // 0 < Actual < Target (◐)
    MISSED,       // Day has passed and actual == 0 (—)
    FUTURE,       // Day has not happened yet (○)
    BEFORE_START  // Day is before goal was created/started
}

data class DayProgress(
    val date: LocalDate,
    val dateString: String, // "YYYY-MM-DD"
    val dayOfMonth: Int,
    val state: DayCompletionState,
    val actualMinutes: Int,
    val targetMinutes: Int,
    val note: String?
) {
    val symbol: String
        get() = when (state) {
            DayCompletionState.COMPLETED -> "✓"
            DayCompletionState.PARTIAL -> "◐"
            DayCompletionState.MISSED -> "—"
            DayCompletionState.FUTURE -> "○"
            DayCompletionState.BEFORE_START -> "·"
        }

    val displaySummary: String
        get() = when (state) {
            DayCompletionState.COMPLETED -> "✓ ${TimeGoalCalculations.formatMinutes(actualMinutes)}"
            DayCompletionState.PARTIAL -> "◐ ${TimeGoalCalculations.formatMinutes(actualMinutes)} / ${TimeGoalCalculations.formatMinutes(targetMinutes)}"
            DayCompletionState.MISSED -> "— Missed"
            DayCompletionState.FUTURE -> "○ Future"
            DayCompletionState.BEFORE_START -> "· Not active"
        }
}

data class MonthlyGoalProgress(
    val goal: TimeGoal,
    val yearMonth: YearMonth,
    val applicableDays: Int,
    val plannedMinutes: Int,
    val recordedMinutes: Int,
    val progressPercent: Int,
    val dailyProgressList: List<DayProgress>,
    val completedDaysCount: Int,
    val partialDaysCount: Int,
    val missedDaysCount: Int,
    val remainingDaysCount: Int
)

data class MonthlyOverallProgress(
    val yearMonth: YearMonth,
    val totalPlannedMinutes: Int,
    val totalRecordedMinutes: Int,
    val overallProgressPercent: Int,
    val goalProgressList: List<MonthlyGoalProgress>,
    val mostTimeSpentGoal: MonthlyGoalProgress?,
    val mostConsistentGoal: MonthlyGoalProgress?,
    val largestGapGoal: MonthlyGoalProgress?
)

object TimeGoalCalculations {
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

    fun formatYearMonth(yearMonth: YearMonth): String {
        return yearMonth.format(MONTH_YEAR_FORMATTER)
    }

    fun parseLocalDate(dateString: String): LocalDate? {
        return try {
            LocalDate.parse(dateString, DATE_FORMATTER)
        } catch (_: Exception) {
            null
        }
    }

    fun formatLocalDate(date: LocalDate): String {
        return date.format(DATE_FORMATTER)
    }

    /**
     * Formats minutes into human-friendly strings:
     * 120 -> "2h"
     * 90 -> "1h 30m"
     * 45 -> "45m"
     * 0 -> "0m"
     */
    fun formatMinutes(minutes: Int): String {
        if (minutes <= 0) return "0m"
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return when {
            hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}m"
            hours > 0 -> "${hours}h"
            else -> "${remainingMinutes}m"
        }
    }

    fun formatHoursTotal(minutes: Int): String {
        val hours = (minutes / 60.0)
        return if (hours % 1.0 == 0.0) {
            "${hours.toInt()}h"
        } else {
            String.format(Locale.US, "%.1fh", hours)
        }
    }

    /**
     * Formats duration in descriptive words with pluralization:
     * 120 -> "2 hours"
     * 60 -> "1 hour"
     * 90 -> "1 hour 30 minutes"
     * 45 -> "45 minutes"
     * 1 -> "1 minute"
     */
    fun formatDurationWords(minutes: Int): String {
        if (minutes <= 0) return "0 minutes"
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return when {
            hours > 0 && remainingMinutes > 0 -> {
                val hourStr = if (hours == 1) "1 hour" else "$hours hours"
                val minStr = if (remainingMinutes == 1) "1 minute" else "$remainingMinutes minutes"
                "$hourStr $minStr"
            }
            hours > 0 -> {
                if (hours == 1) "1 hour" else "$hours hours"
            }
            else -> {
                if (remainingMinutes == 1) "1 minute" else "$remainingMinutes minutes"
            }
        }
    }

    /**
     * Calculates monthly progress for a single goal in a specified YearMonth.
     */
    fun calculateMonthlyGoalProgress(
        goal: TimeGoal,
        yearMonth: YearMonth,
        recordsMap: Map<String, TimeGoalRecord>,
        today: LocalDate = LocalDate.now()
    ): MonthlyGoalProgress {
        val daysInMonth = yearMonth.lengthOfMonth()
        val parsedStart = parseLocalDate(goal.startDate)
        val goalStartDate = parsedStart?.let {
            if (it.year == yearMonth.year && it.month == yearMonth.month) {
                yearMonth.atDay(1)
            } else {
                it
            }
        } ?: LocalDate.of(yearMonth.year, yearMonth.month, 1)

        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth()

        // Compute applicable days in this month
        val applicableDays = when {
            goalStartDate.isAfter(lastDayOfMonth) -> 0
            goalStartDate.isBefore(firstDayOfMonth) || goalStartDate.isEqual(firstDayOfMonth) -> daysInMonth
            else -> (daysInMonth - goalStartDate.dayOfMonth + 1).coerceAtLeast(0)
        }

        val plannedMinutes = applicableDays * goal.dailyTargetMinutes

        val dailyList = mutableListOf<DayProgress>()
        var recordedMinutes = 0
        var completedDays = 0
        var partialDays = 0
        var missedDays = 0
        var remainingDays = 0

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val dateStr = formatLocalDate(date)
            val record = recordsMap[dateStr]
            val actual = record?.actualMinutes ?: 0
            val note = record?.note

            val state: DayCompletionState = when {
                date.isBefore(goalStartDate) -> DayCompletionState.BEFORE_START
                date.isAfter(today) -> DayCompletionState.FUTURE
                actual >= goal.dailyTargetMinutes && goal.dailyTargetMinutes > 0 -> DayCompletionState.COMPLETED
                actual > 0 -> DayCompletionState.PARTIAL
                else -> DayCompletionState.MISSED
            }

            dailyList.add(
                DayProgress(
                    date = date,
                    dateString = dateStr,
                    dayOfMonth = day,
                    state = state,
                    actualMinutes = actual,
                    targetMinutes = goal.dailyTargetMinutes,
                    note = note
                )
            )

            if (state != DayCompletionState.BEFORE_START) {
                recordedMinutes += actual
                when (state) {
                    DayCompletionState.COMPLETED -> completedDays++
                    DayCompletionState.PARTIAL -> partialDays++
                    DayCompletionState.MISSED -> missedDays++
                    DayCompletionState.FUTURE -> remainingDays++
                    else -> {}
                }
            }
        }

        val progressPercent = if (plannedMinutes > 0) {
            ((recordedMinutes.toDouble() / plannedMinutes.toDouble()) * 100.0).roundToInt()
        } else if (recordedMinutes > 0) {
            100
        } else {
            0
        }

        return MonthlyGoalProgress(
            goal = goal,
            yearMonth = yearMonth,
            applicableDays = applicableDays,
            plannedMinutes = plannedMinutes,
            recordedMinutes = recordedMinutes,
            progressPercent = progressPercent,
            dailyProgressList = dailyList,
            completedDaysCount = completedDays,
            partialDaysCount = partialDays,
            missedDaysCount = missedDays,
            remainingDaysCount = remainingDays
        )
    }

    /**
     * Calculates overall monthly totals across all active goals.
     */
    fun calculateMonthlyOverallProgress(
        goals: List<TimeGoal>,
        yearMonth: YearMonth,
        allRecords: List<TimeGoalRecord>,
        today: LocalDate = LocalDate.now()
    ): MonthlyOverallProgress {
        val activeGoals = goals.filter { !it.isArchived }

        val recordsByGoal = allRecords.groupBy { it.goalId }
        val goalProgressList = activeGoals.map { goal ->
            val recordsMap = (recordsByGoal[goal.id] ?: emptyList()).associateBy { it.date }
            calculateMonthlyGoalProgress(goal, yearMonth, recordsMap, today)
        }

        val totalPlanned = goalProgressList.sumOf { it.plannedMinutes }
        val totalRecorded = goalProgressList.sumOf { it.recordedMinutes }

        val overallPercent = if (totalPlanned > 0) {
            ((totalRecorded.toDouble() / totalPlanned.toDouble()) * 100.0).roundToInt()
        } else if (totalRecorded > 0) {
            100
        } else {
            0
        }

        val mostTimeSpent = goalProgressList.maxByOrNull { it.recordedMinutes }
            ?.takeIf { it.recordedMinutes > 0 }

        val mostConsistent = goalProgressList.maxByOrNull { it.completedDaysCount }
            ?.takeIf { it.completedDaysCount > 0 }

        val largestGap = goalProgressList.maxByOrNull { (it.plannedMinutes - it.recordedMinutes).coerceAtLeast(0) }
            ?.takeIf { (it.plannedMinutes - it.recordedMinutes) > 0 }

        return MonthlyOverallProgress(
            yearMonth = yearMonth,
            totalPlannedMinutes = totalPlanned,
            totalRecordedMinutes = totalRecorded,
            overallProgressPercent = overallPercent,
            goalProgressList = goalProgressList,
            mostTimeSpentGoal = mostTimeSpent,
            mostConsistentGoal = mostConsistent,
            largestGapGoal = largestGap
        )
    }

    /**
     * Calculates the target destination index when dragging an item vertically in a list.
     * Accurately determines boundary threshold crossings based on cumulative item heights and spacing.
     */
    fun calculateDropIndex(
        fromIndex: Int,
        dragOffsetY: Float,
        itemCount: Int,
        itemHeights: Map<Int, Float> = emptyMap(),
        defaultSlotHeightPx: Float = 172f
    ): Int {
        if (fromIndex !in 0 until itemCount || itemCount <= 1) return fromIndex

        if (dragOffsetY > 0f) {
            var accumulated = 0f
            var target = fromIndex
            for (i in (fromIndex + 1) until itemCount) {
                val slot = itemHeights[i] ?: defaultSlotHeightPx
                if (dragOffsetY >= accumulated + slot * 0.5f) {
                    target = i
                    accumulated += slot
                } else {
                    break
                }
            }
            return target.coerceIn(0, itemCount - 1)
        } else if (dragOffsetY < 0f) {
            var accumulated = 0f
            var target = fromIndex
            val absDrag = -dragOffsetY
            for (i in (fromIndex - 1) downTo 0) {
                val slot = itemHeights[i] ?: defaultSlotHeightPx
                if (absDrag >= accumulated + slot * 0.5f) {
                    target = i
                    accumulated += slot
                } else {
                    break
                }
            }
            return target.coerceIn(0, itemCount - 1)
        }
        return fromIndex
    }
}
