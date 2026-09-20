package com.example.gentlenudge

import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.notification.NudgeAlarmScheduler
import com.example.gentlenudge.ui.components.TaskOccurrenceResolver
import com.example.gentlenudge.ui.components.calculateTimeRemainingText
import com.example.gentlenudge.ui.components.isTaskOnDateHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MidnightDateBoundaryTest {

    private fun makeCalendar(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // 0-based month
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun test1_Sept10_1159PM_ReminderSept11_500AM_IsLater() {
        val nowCal = makeCalendar(2026, 9, 10, 23, 59)
        val targetCal = makeCalendar(2026, 9, 11, 5, 0)

        val task = NudgeTask(
            id = 101L,
            title = "Morning Coffee",
            dateLabel = "Tomorrow",
            timeLabel = "5:00 AM",
            createdAt = nowCal.timeInMillis
        )

        val resolvedTarget = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(
            task = task,
            context = null,
            nowMillis = nowCal.timeInMillis
        )

        assertEquals("Resolved target must be Sept 11 5:00 AM", targetCal.timeInMillis, resolvedTarget)
        val isToday = TaskOccurrenceResolver.isOccurrenceTodayOrPast(resolvedTarget, nowCal.timeInMillis)
        assertFalse("At 11:59 PM Sept 10, reminder for Sept 11 must be in Later, not Today", isToday)
    }

    @Test
    fun test2_Sept11_1200AM_SameReminderSept11_500AM_IsToday() {
        val createdCal = makeCalendar(2026, 9, 10, 23, 59)
        val nowCal = makeCalendar(2026, 9, 11, 0, 0)
        val targetCal = makeCalendar(2026, 9, 11, 5, 0)

        val task = NudgeTask(
            id = 101L,
            title = "Morning Coffee",
            dateLabel = "Tomorrow",
            timeLabel = "5:00 AM",
            createdAt = createdCal.timeInMillis
        )

        val resolvedTarget = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(
            task = task,
            context = null,
            nowMillis = nowCal.timeInMillis
        )

        assertEquals("Resolved target must still be Sept 11 5:00 AM", targetCal.timeInMillis, resolvedTarget)
        val isToday = TaskOccurrenceResolver.isOccurrenceTodayOrPast(resolvedTarget, nowCal.timeInMillis)
        assertTrue("At 12:00 AM Sept 11, reminder for Sept 11 must now be in Today's Nudges", isToday)
    }

    @Test
    fun test3_Sept11_454AM_ReminderSept11_500AM_CountdownIs6Minutes() {
        val createdCal = makeCalendar(2026, 9, 10, 23, 0)
        val targetCal = makeCalendar(2026, 9, 11, 5, 0)

        val task = NudgeTask(
            id = 102L,
            title = "Early Flight",
            dateLabel = "Tomorrow",
            timeLabel = "5:00 AM",
            createdAt = createdCal.timeInMillis
        )

        val resolvedTarget = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(
            task = task,
            context = null,
            nowMillis = makeCalendar(2026, 9, 11, 4, 54).timeInMillis
        )
        val diffMinutes = (resolvedTarget - makeCalendar(2026, 9, 11, 4, 54).timeInMillis) / 60000

        assertEquals("Countdown must be exactly 6 minutes, not 1 day 6 minutes", 6L, diffMinutes)
    }

    @Test
    fun test4_Sept11_501AM_OneTimeReminder_RemainsSept11_DoesNotRollForward() {
        val createdCal = makeCalendar(2026, 9, 10, 20, 0)
        val targetCal = makeCalendar(2026, 9, 11, 5, 0)
        val nowCal = makeCalendar(2026, 9, 11, 5, 1)

        val task = NudgeTask(
            id = 103L,
            title = "Take Medicine",
            dateLabel = "Tomorrow",
            timeLabel = "5:00 AM",
            createdAt = createdCal.timeInMillis
        )

        val resolvedTarget = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(
            task = task,
            context = null,
            nowMillis = nowCal.timeInMillis
        )

        assertEquals("One-time reminder must NOT roll forward to Sept 12", targetCal.timeInMillis, resolvedTarget)
        val isTodayOrPast = TaskOccurrenceResolver.isOccurrenceTodayOrPast(resolvedTarget, nowCal.timeInMillis)
        assertTrue("Overdue one-time reminder from earlier today remains in Today's Nudges", isTodayOrPast)
    }

    @Test
    fun test5_Sept11_1159PM_ReminderSept12_500AM_IsLater() {
        val nowCal = makeCalendar(2026, 9, 11, 23, 59)
        val targetCal = makeCalendar(2026, 9, 12, 5, 0)

        val task = NudgeTask(
            id = 104L,
            title = "Morning Workout",
            dateLabel = "Tomorrow",
            timeLabel = "5:00 AM",
            createdAt = nowCal.timeInMillis
        )

        val resolvedTarget = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(
            task = task,
            context = null,
            nowMillis = nowCal.timeInMillis
        )

        assertEquals("Resolved target must be Sept 12 5:00 AM", targetCal.timeInMillis, resolvedTarget)
        val isToday = TaskOccurrenceResolver.isOccurrenceTodayOrPast(resolvedTarget, nowCal.timeInMillis)
        assertFalse("At 11:59 PM Sept 11, reminder for Sept 12 must be in Later", isToday)
    }

    @Test
    fun test6_Sept12_1201AM_SameReminderSept12_500AM_IsToday() {
        val createdCal = makeCalendar(2026, 9, 11, 23, 59)
        val nowCal = makeCalendar(2026, 9, 12, 0, 1)
        val targetCal = makeCalendar(2026, 9, 12, 5, 0)

        val task = NudgeTask(
            id = 104L,
            title = "Morning Workout",
            dateLabel = "Tomorrow",
            timeLabel = "5:00 AM",
            createdAt = createdCal.timeInMillis
        )

        val resolvedTarget = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(
            task = task,
            context = null,
            nowMillis = nowCal.timeInMillis
        )

        assertEquals("Resolved target must still be Sept 12 5:00 AM", targetCal.timeInMillis, resolvedTarget)
        val isToday = TaskOccurrenceResolver.isOccurrenceTodayOrPast(resolvedTarget, nowCal.timeInMillis)
        assertTrue("At 12:01 AM Sept 12, reminder for Sept 12 must be in Today's Nudges", isToday)
    }

    @Test
    fun test7_EveryDay_500AM_OccurrenceTransitions() {
        val task = NudgeTask(
            id = 105L,
            title = "Daily Standup",
            dateLabel = "Today",
            timeLabel = "5:00 AM",
            repeat = "Every day"
        )

        // 1. Sept 11, 4:54 AM -> next occurrence is Sept 11, 5:00 AM -> Today
        val now1 = makeCalendar(2026, 9, 11, 4, 54)
        val occ1 = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(task, null, now1.timeInMillis)
        val expected1 = makeCalendar(2026, 9, 11, 5, 0).timeInMillis
        assertEquals("At 4:54 AM, next occurrence is today at 5:00 AM", expected1, occ1)
        assertTrue("At 4:54 AM, daily reminder is Today", TaskOccurrenceResolver.isOccurrenceToday(occ1, now1.timeInMillis))

        // 2. Sept 11, 5:01 AM -> next occurrence is Sept 12, 5:00 AM -> Tomorrow / Later
        val now2 = makeCalendar(2026, 9, 11, 5, 1)
        val occ2 = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(task, null, now2.timeInMillis)
        val expected2 = makeCalendar(2026, 9, 12, 5, 0).timeInMillis
        assertEquals("At 5:01 AM, next occurrence rolls to tomorrow at 5:00 AM", expected2, occ2)
        assertFalse("At 5:01 AM, next occurrence is not Today", TaskOccurrenceResolver.isOccurrenceToday(occ2, now2.timeInMillis))

        // 3. Sept 12, 12:01 AM -> next occurrence is Sept 12, 5:00 AM -> Today
        val now3 = makeCalendar(2026, 9, 12, 0, 1)
        val occ3 = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(task, null, now3.timeInMillis)
        assertEquals("At 12:01 AM next day, next occurrence is Sept 12 5:00 AM", expected2, occ3)
        assertTrue("At 12:01 AM on Sept 12, daily reminder is Today", TaskOccurrenceResolver.isOccurrenceToday(occ3, now3.timeInMillis))
    }

    @Test
    fun test8_MidnightBoundary_DayOverviewMatching() {
        val createdCal = makeCalendar(2026, 9, 10, 23, 0)
        val targetCal = makeCalendar(2026, 9, 11, 5, 0)

        val task = NudgeTask(
            id = 106L,
            title = "Client Presentation",
            dateLabel = "Tomorrow",
            timeLabel = "5:00 AM",
            createdAt = createdCal.timeInMillis
        )

        // Across midnight:
        val daySept11 = makeCalendar(2026, 9, 11, 12, 0)
        val daySept12 = makeCalendar(2026, 9, 12, 12, 0)

        // Check DayOverview matching on Sept 11
        val matchesSept11 = isTaskOnDateHelper(task, daySept11)
        assertTrue("Task scheduled for Sept 11 5:00 AM MUST match Day Overview for Sept 11", matchesSept11)

        // Check DayOverview matching on Sept 12
        val matchesSept12 = isTaskOnDateHelper(task, daySept12)
        assertFalse("Task scheduled for Sept 11 5:00 AM MUST NOT match Day Overview for Sept 12", matchesSept12)

        // Verify day boundary detection
        val t1 = makeCalendar(2026, 9, 10, 23, 59).timeInMillis
        val t2 = makeCalendar(2026, 9, 11, 0, 0).timeInMillis
        assertFalse("Sept 10 11:59 PM and Sept 11 12:00 AM must NOT be recognized as the same day",
            TaskOccurrenceResolver.isSameDay(t1, t2))
    }

    @Test
    fun testDayOverview_1_Sept10_1159PM_ReminderSept11_MatchesSept11() {
        val createdCal = makeCalendar(2026, 9, 10, 23, 59)
        val task = NudgeTask(
            id = 201L,
            title = "Doctor Appointment",
            dateLabel = "Tomorrow",
            timeLabel = "5:00 AM",
            createdAt = createdCal.timeInMillis
        )
        val daySept11 = makeCalendar(2026, 9, 11, 10, 0)
        assertTrue(isTaskOnDateHelper(task, daySept11))
    }

    @Test
    fun testDayOverview_2_Sept11_1201AM_SameReminder_MatchesSept11() {
        val createdCal = makeCalendar(2026, 9, 10, 23, 59)
        val task = NudgeTask(
            id = 201L,
            title = "Doctor Appointment",
            dateLabel = "Tomorrow",
            timeLabel = "5:00 AM",
            createdAt = createdCal.timeInMillis
        )
        val daySept11 = makeCalendar(2026, 9, 11, 10, 0)
        assertTrue(isTaskOnDateHelper(task, daySept11))
    }

    @Test
    fun testDayOverview_3_Sept11_1201AM_SameReminder_DoesNotMatchSept12() {
        val createdCal = makeCalendar(2026, 9, 10, 23, 59)
        val task = NudgeTask(
            id = 201L,
            title = "Doctor Appointment",
            dateLabel = "Tomorrow",
            timeLabel = "5:00 AM",
            createdAt = createdCal.timeInMillis
        )
        val daySept12 = makeCalendar(2026, 9, 12, 10, 0)
        assertFalse(isTaskOnDateHelper(task, daySept12))
    }

    @Test
    fun testDayOverview_4_EveryDay_500AM_FollowsNextOccurrence() {
        val task = NudgeTask(
            id = 202L,
            title = "Daily Meditation",
            dateLabel = "Today",
            timeLabel = "5:00 AM",
            repeat = "Every day"
        )
        val occAt454 = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(
            task = task,
            context = null,
            nowMillis = makeCalendar(2026, 9, 11, 4, 54).timeInMillis
        )
        val occAt501 = TaskOccurrenceResolver.resolveTargetOccurrenceMillis(
            task = task,
            context = null,
            nowMillis = makeCalendar(2026, 9, 11, 5, 1).timeInMillis
        )

        val calSept11 = makeCalendar(2026, 9, 11, 12, 0)
        val calSept12 = makeCalendar(2026, 9, 12, 12, 0)

        // At 4:54 AM, occurrence is Sept 11
        val occCal1 = Calendar.getInstance().apply { timeInMillis = occAt454 }
        assertEquals(calSept11.get(Calendar.DAY_OF_MONTH), occCal1.get(Calendar.DAY_OF_MONTH))

        // At 5:01 AM, occurrence is Sept 12
        val occCal2 = Calendar.getInstance().apply { timeInMillis = occAt501 }
        assertEquals(calSept12.get(Calendar.DAY_OF_MONTH), occCal2.get(Calendar.DAY_OF_MONTH))
    }
}
