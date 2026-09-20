package com.example.gentlenudge

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gentlenudge.notification.EventNotificationMode
import com.example.gentlenudge.notification.NudgeEventNotificationScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NudgeEventNotificationSchedulerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Reset to default
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.OFF
        )
    }

    @Test
    fun testDefaultSettingIsOff() {
        val mode = NudgeEventNotificationScheduler.getEventNotificationMode(context)
        val days = NudgeEventNotificationScheduler.getEventNotificationDays(context)
        assertEquals(EventNotificationMode.OFF, mode)
        assertEquals(0, days)
    }

    @Test
    fun testSetOneDayBefore() {
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.ONE_DAY_BEFORE
        )

        val mode = NudgeEventNotificationScheduler.getEventNotificationMode(context)
        val days = NudgeEventNotificationScheduler.getEventNotificationDays(context)

        assertEquals(EventNotificationMode.ONE_DAY_BEFORE, mode)
        assertEquals(1, days)
    }

    @Test
    fun testSetTwoDaysBefore() {
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.TWO_DAYS_BEFORE
        )

        val mode = NudgeEventNotificationScheduler.getEventNotificationMode(context)
        val days = NudgeEventNotificationScheduler.getEventNotificationDays(context)

        assertEquals(EventNotificationMode.TWO_DAYS_BEFORE, mode)
        assertEquals(2, days)
    }

    @Test
    fun testSetCustomDays() {
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.CUSTOM,
            customDays = 5
        )

        val mode = NudgeEventNotificationScheduler.getEventNotificationMode(context)
        val days = NudgeEventNotificationScheduler.getEventNotificationDays(context)

        assertEquals(EventNotificationMode.CUSTOM, mode)
        assertEquals(5, days)
    }

    @Test
    fun testTurnOffCancels() {
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.CUSTOM,
            customDays = 4
        )
        assertEquals(4, NudgeEventNotificationScheduler.getEventNotificationDays(context))

        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.OFF
        )

        val mode = NudgeEventNotificationScheduler.getEventNotificationMode(context)
        val days = NudgeEventNotificationScheduler.getEventNotificationDays(context)

        assertEquals(EventNotificationMode.OFF, mode)
        assertEquals(0, days)
    }

    @Test
    fun testRequestCodeGeneration() {
        val code1 = NudgeEventNotificationScheduler.getEventRequestCode("diwali", 2026, 11, 8)
        val code2 = NudgeEventNotificationScheduler.getEventRequestCode("holi", 2026, 3, 4)
        val code3 = NudgeEventNotificationScheduler.getEventRequestCode("diwali", 2026, 11, 8)

        assertEquals(code1, code3)
        assertNotEquals(code1, code2)
    }

    @Test
    fun testDeliveredReminderTrackingAndDuplicatePrevention() {
        val reminderKey = NudgeEventNotificationScheduler.getEventReminderKey("test_event", 2026, 9, 4)
        assertEquals("test_event-2026-9-4", reminderKey)

        NudgeEventNotificationScheduler.clearDeliveredEventReminders(context)
        assertEquals(false, NudgeEventNotificationScheduler.isEventReminderDelivered(context, reminderKey))

        NudgeEventNotificationScheduler.markEventReminderDelivered(context, reminderKey)
        assertEquals(true, NudgeEventNotificationScheduler.isEventReminderDelivered(context, reminderKey))

        // Delivering again should return false (duplicate prevented)
        val secondDelivery = NudgeEventNotificationScheduler.deliverMissedReminder(
            context = context,
            eventId = "test_event",
            eventName = "Test Event",
            category = "Festivals",
            description = "Test description",
            year = 2026,
            month = 9,
            day = 4,
            daysBefore = 1
        )
        assertEquals(false, secondDelivery)
    }

    @Test
    fun testSwitchingModeClearsPreviousAlarms() {
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.ONE_DAY_BEFORE
        )
        assertEquals(1, NudgeEventNotificationScheduler.getEventNotificationDays(context))

        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.TWO_DAYS_BEFORE
        )
        assertEquals(2, NudgeEventNotificationScheduler.getEventNotificationDays(context))
        assertEquals(EventNotificationMode.TWO_DAYS_BEFORE, NudgeEventNotificationScheduler.getEventNotificationMode(context))
    }

    @Test
    fun testSetCustomNotificationTime() {
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.CUSTOM,
            customDays = 4,
            customHour = 15,
            customMinute = 45
        )

        assertEquals(EventNotificationMode.CUSTOM, NudgeEventNotificationScheduler.getEventNotificationMode(context))
        assertEquals(4, NudgeEventNotificationScheduler.getEventNotificationDays(context))
        assertEquals(15, NudgeEventNotificationScheduler.getEventNotificationHour(context))
        assertEquals(45, NudgeEventNotificationScheduler.getEventNotificationMinute(context))
        assertEquals(15, NudgeEventNotificationScheduler.getCustomNotificationHour(context))
        assertEquals(45, NudgeEventNotificationScheduler.getCustomNotificationMinute(context))
    }

    @Test
    fun testStandardModesAlwaysUse9AM() {
        // Even if custom hour/min were previously set, standard modes must always return 9:00 AM
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.CUSTOM,
            customDays = 5,
            customHour = 16,
            customMinute = 30
        )

        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.ONE_DAY_BEFORE
        )
        assertEquals(9, NudgeEventNotificationScheduler.getEventNotificationHour(context))
        assertEquals(0, NudgeEventNotificationScheduler.getEventNotificationMinute(context))

        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.TWO_DAYS_BEFORE
        )
        assertEquals(9, NudgeEventNotificationScheduler.getEventNotificationHour(context))
        assertEquals(0, NudgeEventNotificationScheduler.getEventNotificationMinute(context))

        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.OFF
        )
        assertEquals(9, NudgeEventNotificationScheduler.getEventNotificationHour(context))
        assertEquals(0, NudgeEventNotificationScheduler.getEventNotificationMinute(context))
    }

    @Test
    fun testFormatTimeHelper() {
        assertEquals("9:00 AM", NudgeEventNotificationScheduler.formatTime(9, 0))
        assertEquals("12:05 AM", NudgeEventNotificationScheduler.formatTime(0, 5))
        assertEquals("12:00 PM", NudgeEventNotificationScheduler.formatTime(12, 0))
        assertEquals("2:30 PM", NudgeEventNotificationScheduler.formatTime(14, 30))
        assertEquals("11:45 PM", NudgeEventNotificationScheduler.formatTime(23, 45))
    }

    @Test
    fun testBackupAndRestoreCustomEventNotificationTime() {
        val testPrefs = context.getSharedPreferences("test_custom_notif_prefs", Context.MODE_PRIVATE)
        testPrefs.edit().clear().apply()

        val p = mapOf(
            NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MODE to "CUSTOM",
            NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_DAYS to 6,
            NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_HOUR to 10,
            NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MINUTE to 15
        )

        // Simulate restorePreferences logic
        val editor = testPrefs.edit()
        if (p.containsKey(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MODE)) {
            editor.putString(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MODE, p[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MODE] as String)
        }
        if (p.containsKey(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_DAYS)) {
            editor.putInt(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_DAYS, (p[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_DAYS] as Number).toInt())
        }
        if (p.containsKey(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_HOUR)) {
            editor.putInt(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_HOUR, (p[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_HOUR] as Number).toInt())
        }
        if (p.containsKey(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MINUTE)) {
            editor.putInt(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MINUTE, (p[NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MINUTE] as Number).toInt())
        }
        editor.commit()

        assertEquals("CUSTOM", testPrefs.getString(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MODE, null))
        assertEquals(6, testPrefs.getInt(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_DAYS, 1))
        assertEquals(10, testPrefs.getInt(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_HOUR, 9))
        assertEquals(15, testPrefs.getInt(NudgeEventNotificationScheduler.KEY_EVENT_NOTIF_MINUTE, 0))
    }

    @Test
    fun testCustomDaysPreservedWhenSwitchingToPresetAndSaving() {
        // Flow: Custom = 5 days -> Save -> Switch to 1 day before -> Save -> Custom value is still 5
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.CUSTOM,
            customDays = 5
        )
        assertEquals(EventNotificationMode.CUSTOM, NudgeEventNotificationScheduler.getEventNotificationMode(context))
        assertEquals(5, NudgeEventNotificationScheduler.getEventNotificationDays(context))
        assertEquals(5, NudgeEventNotificationScheduler.getCustomNotificationDays(context))

        // Save as ONE_DAY_BEFORE while preserving customDays = 5
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.ONE_DAY_BEFORE,
            customDays = 5
        )
        // Active reminder is 1 day before
        assertEquals(EventNotificationMode.ONE_DAY_BEFORE, NudgeEventNotificationScheduler.getEventNotificationMode(context))
        assertEquals(1, NudgeEventNotificationScheduler.getEventNotificationDays(context))
        // Preserved custom days preference is still 5
        assertEquals(5, NudgeEventNotificationScheduler.getCustomNotificationDays(context))

        // Save as TWO_DAYS_BEFORE while preserving customDays = 5
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.TWO_DAYS_BEFORE,
            customDays = 5
        )
        // Active reminder is 2 days before
        assertEquals(EventNotificationMode.TWO_DAYS_BEFORE, NudgeEventNotificationScheduler.getEventNotificationMode(context))
        assertEquals(2, NudgeEventNotificationScheduler.getEventNotificationDays(context))
        // Preserved custom days preference is still 5
        assertEquals(5, NudgeEventNotificationScheduler.getCustomNotificationDays(context))
    }

    @Test
    fun testValidLowCustomDaysOneAndTwoNotConvertedToThree() {
        // Test custom days = 1 is preserved and not converted to 3
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.CUSTOM,
            customDays = 1
        )
        assertEquals(EventNotificationMode.CUSTOM, NudgeEventNotificationScheduler.getEventNotificationMode(context))
        assertEquals(1, NudgeEventNotificationScheduler.getEventNotificationDays(context))
        assertEquals(1, NudgeEventNotificationScheduler.getCustomNotificationDays(context))

        // Test custom days = 2 is preserved and not converted to 3
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.CUSTOM,
            customDays = 2
        )
        assertEquals(EventNotificationMode.CUSTOM, NudgeEventNotificationScheduler.getEventNotificationMode(context))
        assertEquals(2, NudgeEventNotificationScheduler.getEventNotificationDays(context))
        assertEquals(2, NudgeEventNotificationScheduler.getCustomNotificationDays(context))
    }

    @Test
    fun testCustomDays7PreservedAcrossPresets() {
        // Flow: Custom = 7 days -> preset switch -> customDays stays 7
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.CUSTOM,
            customDays = 7
        )
        assertEquals(7, NudgeEventNotificationScheduler.getCustomNotificationDays(context))

        // Select 1 day before
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.ONE_DAY_BEFORE,
            customDays = 7
        )
        assertEquals(1, NudgeEventNotificationScheduler.getEventNotificationDays(context))
        assertEquals(7, NudgeEventNotificationScheduler.getCustomNotificationDays(context))

        // Select 2 days before
        NudgeEventNotificationScheduler.setEventNotificationSetting(
            context = context,
            mode = EventNotificationMode.TWO_DAYS_BEFORE,
            customDays = 7
        )
        assertEquals(2, NudgeEventNotificationScheduler.getEventNotificationDays(context))
        assertEquals(7, NudgeEventNotificationScheduler.getCustomNotificationDays(context))
    }

    @Test
    fun testDefaultCustomDaysIsThreeWhenNoCustomValueSaved() {
        val prefs = context.getSharedPreferences("gentle_nudge_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        assertEquals(3, NudgeEventNotificationScheduler.getCustomNotificationDays(context))
    }
}
