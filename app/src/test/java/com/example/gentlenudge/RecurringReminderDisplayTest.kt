package com.example.gentlenudge

import com.example.gentlenudge.ui.components.RecurringReminderUiHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RecurringReminderDisplayTest {

    @Test
    fun testPreviousDayBeforeMidnight_DisplaysTomorrow() {
        // 11:59 PM on Monday for a 5:00 AM daily reminder
        val now = LocalDateTime.of(2026, 9, 7, 23, 59, 0)
        val display = RecurringReminderUiHelper.getRecurringReminderDisplay(
            timeLabel = "5:00 AM",
            repeatRule = "Every day",
            now = now
        )

        assertNotNull("Display should not be null for daily recurring reminder", display)
        assertEquals(
            "Next occurrence should be tomorrow at 5:00 AM",
            LocalDateTime.of(2026, 9, 8, 5, 0, 0),
            display!!.nextOccurrence
        )
        assertEquals("Relative date label must be Tomorrow", "Tomorrow", display.relativeDateLabel)
        assertEquals("Countdown must be 5h 1m", "5h 1m", display.countdown)
        assertEquals("Display string must be Tomorrow, 5h 1m", "Tomorrow, 5h 1m", display.displayString)
    }

    @Test
    fun testJustAfterMidnight_DisplaysToday() {
        // 12:01 AM on Tuesday for a 5:00 AM daily reminder
        val now = LocalDateTime.of(2026, 9, 8, 0, 1, 0)
        val display = RecurringReminderUiHelper.getRecurringReminderDisplay(
            timeLabel = "5:00 AM",
            repeatRule = "Every day",
            now = now
        )

        assertNotNull(display)
        assertEquals(
            "Next occurrence should be today at 5:00 AM",
            LocalDateTime.of(2026, 9, 8, 5, 0, 0),
            display!!.nextOccurrence
        )
        assertEquals("Relative date label must be Today", "Today", display.relativeDateLabel)
        assertEquals("Countdown must be 4h 59m", "4h 59m", display.countdown)
        assertEquals("Display string must be Today, 4h 59m", "Today, 4h 59m", display.displayString)
    }

    @Test
    fun testFourAmForFiveAmReminder_DisplaysTodayOneHour() {
        // 4:00 AM on Tuesday for a 5:00 AM daily reminder
        val now = LocalDateTime.of(2026, 9, 8, 4, 0, 0)
        val display = RecurringReminderUiHelper.getRecurringReminderDisplay(
            timeLabel = "5:00 AM",
            repeatRule = "Every day",
            now = now
        )

        assertNotNull(display)
        assertEquals(
            "Next occurrence should be today at 5:00 AM",
            LocalDateTime.of(2026, 9, 8, 5, 0, 0),
            display!!.nextOccurrence
        )
        assertEquals("Relative date label must be Today", "Today", display.relativeDateLabel)
        assertEquals("Countdown must be 1h", "1h", display.countdown)
        assertEquals("Display string must be Today, 1h", "Today, 1h", display.displayString)
    }

    @Test
    fun testFourFiftyEightAmForFiveAmReminder_DisplaysTodayTwoMinutes() {
        // 4:58 AM on Tuesday for a 5:00 AM daily reminder
        val now = LocalDateTime.of(2026, 9, 8, 4, 58, 0)
        val display = RecurringReminderUiHelper.getRecurringReminderDisplay(
            timeLabel = "5:00 AM",
            repeatRule = "Every day",
            now = now
        )

        assertNotNull(display)
        assertEquals(
            "Next occurrence should be today at 5:00 AM",
            LocalDateTime.of(2026, 9, 8, 5, 0, 0),
            display!!.nextOccurrence
        )
        assertEquals("Relative date label must be Today", "Today", display.relativeDateLabel)
        assertEquals("Countdown must be 2m", "2m", display.countdown)
        assertEquals("Display string must be Today, 2m", "Today, 2m", display.displayString)
    }

    @Test
    fun testExactlyAtScheduledOccurrence_AdvancesToTomorrowOccurrence() {
        // Exactly at 5:00 AM on Tuesday for a 5:00 AM daily reminder
        val now = LocalDateTime.of(2026, 9, 8, 5, 0, 0)
        val display = RecurringReminderUiHelper.getRecurringReminderDisplay(
            timeLabel = "5:00 AM",
            repeatRule = "Every day",
            now = now
        )

        assertNotNull(display)
        assertEquals(
            "Next occurrence must advance to tomorrow at 5:00 AM",
            LocalDateTime.of(2026, 9, 9, 5, 0, 0),
            display!!.nextOccurrence
        )
        assertEquals("Relative date label must be Tomorrow", "Tomorrow", display.relativeDateLabel)
        assertEquals("Countdown must be 24h", "24h", display.countdown)
        assertEquals("Display string must be Tomorrow, 24h", "Tomorrow, 24h", display.displayString)
    }

    @Test
    fun testJustAfterScheduledOccurrence_AdvancesToTomorrowOccurrence() {
        // 5:01 AM on Tuesday for a 5:00 AM daily reminder
        val now = LocalDateTime.of(2026, 9, 8, 5, 1, 0)
        val display = RecurringReminderUiHelper.getRecurringReminderDisplay(
            timeLabel = "5:00 AM",
            repeatRule = "Every day",
            now = now
        )

        assertNotNull(display)
        assertEquals(
            "Next occurrence must be tomorrow at 5:00 AM",
            LocalDateTime.of(2026, 9, 9, 5, 0, 0),
            display!!.nextOccurrence
        )
        assertEquals("Relative date label must be Tomorrow", "Tomorrow", display.relativeDateLabel)
        assertEquals("Countdown must be 23h 59m", "23h 59m", display.countdown)
        assertEquals("Display string must be Tomorrow, 23h 59m", "Tomorrow, 23h 59m", display.displayString)
    }

    @Test
    fun testSameCalculatedTimestampUsedForBothDateLabelAndCountdown() {
        val now = LocalDateTime.of(2026, 9, 8, 3, 30, 0)
        val scheduledTime = LocalTime.of(5, 0)
        val nextOccurrence = RecurringReminderUiHelper.calculateNextOccurrence(now, scheduledTime)

        val relativeDate = RecurringReminderUiHelper.calculateRelativeDateLabel(now.toLocalDate(), nextOccurrence.toLocalDate())
        val countdown = RecurringReminderUiHelper.calculateCountdown(now, nextOccurrence)

        assertEquals("Today", relativeDate)
        assertEquals("1h 30m", countdown)
    }

    @Test
    fun testNonRecurringReminders_ReturnNull() {
        val displayNoRepeat = RecurringReminderUiHelper.getRecurringReminderDisplay(
            timeLabel = "5:00 AM",
            repeatRule = "Does not repeat",
            now = LocalDateTime.now()
        )
        assertNull(displayNoRepeat)

        val displayOnce = RecurringReminderUiHelper.getRecurringReminderDisplay(
            timeLabel = "5:00 AM",
            repeatRule = "Once",
            now = LocalDateTime.now()
        )
        assertNull(displayOnce)
    }
}
